package io.psyllabus.platform.auth.services;

import io.psyllabus.platform.auth.dto.request.LoginRequest;
import io.psyllabus.platform.auth.dto.request.RegisterRequest;
import io.psyllabus.platform.auth.dto.responses.AuthResult;
import io.psyllabus.platform.auth.dto.responses.LoginResponse;
import io.psyllabus.platform.auth.dto.responses.UserResponse;
import io.psyllabus.platform.auth.entities.RefreshToken;
import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.auth.repository.UserRepository;
import io.psyllabus.platform.common.constants.AuthConstants;
import io.psyllabus.platform.common.exception.*;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.school.entities.School;
import io.psyllabus.platform.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of AuthService.
 * Handles login (dual-token), refresh, logout, and registration.
 *
 * Token strategy:
 *   Access token  — short-lived JWT, returned in JSON body
 *   Refresh token — longer-lived JWT + DB revocation record (hybrid)
 *                   returned to controller, controller sets HttpOnly cookie
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    public static final String TOKEN_TYPE = "Bearer";
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final StringRedisTemplate redisTemplate;
    private final CacheManager cacheManager;

    @Override
    public AuthResult login(LoginRequest request) {
        log.info("Login attempt for username: {}", request.username());

        // Step 1: Get Tenant from Context (Resolved by TenantFilter)
        UUID tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new TenantNotFoundException("Tenant context not set. Access via valid subdomain or custom domain.");
        }

        // Step 2: Find School (Tenant)
        School school = schoolRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("School not found for ID: " + tenantId));

        log.debug("Login for tenant: {} ({})", school.getName(), school.getId());

        // Step 3: Find user by identifier (username, email, or admission number)
        User user = userRepository.findByIdentifierAndTenantId(request.username(), tenantId)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        // Step 4: Verify Password
        if (!passwordService.verifyPassword(request.password(), user.getPassword())) {
            log.warn("Invalid password for user: {}", request.username());
            throw new UnauthorizedException("Invalid credentials");
        }

        // Step 5: Check user status
        if (!user.isEnabled()) {
            log.warn("Login attempt for disabled user: {}", request.username());
            throw new UnauthorizedException("Account is disabled");
        }

        if (Boolean.TRUE.equals(user.getMustChangePassword())){
           return initiateForcePasswordReset(user);
        }



        /*
        If the user is already logged in (has active refresh tokens), this rows
        are marked revoked here. This enforces single-session-per-user:
        - No stale rows accumulate in refresh_tokens
        - Any previously issued refresh token immediately becomes invalid
        - The user is effectively logged out of all other devices/tabs
         */
        refreshTokenService.revokeAllForUser(user.getId(), tenantId);

        // Step 6: Generate Minimal JWT accessToken (Identity Only) + refresh token
        String accessToken = jwtService.generateAccessToken(user, school);
        String refreshToken = jwtService.generateRefreshToken(user, school);

        log.info("Login successful for user: {} (tenant: {})", user.getUsername(), school.getName());

        // Step 7: Return Minimal Login Response
     LoginResponse loginResponse = new LoginResponse(
                accessToken,
             TOKEN_TYPE,
                jwtService.getAccessTokenExpiration(),
                user.getId(),
                user.getUsername(), false, null
        );
     return new AuthResult(loginResponse, refreshToken);
    }

    @Override
    @Transactional
    public AuthResult completePasswordReset(String changeToken, String newPassword) {
        String userId = validateResetToken(changeToken);
        User user = loadUserFromToken(userId);

        updateUserPassword(user, newPassword);
        invalidateResetToken(changeToken);

        return generateAuthResult(user);
    }

    @Override
    @Transactional(readOnly = true)
    public String refresh(String refreshJwt, UUID tenantId) {
        // Validate JWT signature + revocation (two-gate check in RefreshTokenService)
        RefreshToken tokenRecord = refreshTokenService.validateAndGet(refreshJwt, tenantId);

        // Load user and school to generate new access token
        User user = userRepository.findById(tokenRecord.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        School school = schoolRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("School not found: " + tenantId));

        String newAccessToken = jwtService.generateAccessToken(user, school);

        log.info("Access token refreshed for user: {} (tenant: {})", user.getId(), tenantId);
        return newAccessToken;
    }


    @Override
    public void logout(String refreshJwt, UUID tenantId) {
        try {
            UUID userId = jwtService.extractUserId(refreshJwt);
            Objects.requireNonNull(cacheManager.getCache("userProfile")).evict(userId);
            Objects.requireNonNull(cacheManager.getCache("userClaims")).evict(userId);
        } catch (Exception e) {
            // token may be malformed/expired — eviction is best-effort on logout
            log.debug("Cache eviction skipped on logout — could not extract userId: {}", e.getMessage());
        }

        refreshTokenService.revoke(refreshJwt, tenantId);
        log.info("User logged out (refresh token revoked), tenant: {}", tenantId);
    }

    @Override
    public UserResponse register(RegisterRequest request) {

        log.info("Registration attempt for: {}", request.username());

        UUID tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new TenantNotFoundException("Tenant context not set");
        }

        // Validate uniqueness
        if (userRepository.existsByUsernameAndTenantId(request.username(), tenantId)) {
            throw new DuplicateResourceException("Username already exists");
        }

        if (userRepository.existsByEmailAndTenantId(request.email(), tenantId)) {
            throw new DuplicateResourceException("Email already exists");
        }

        // Validate password strength
        if (!passwordService.isPasswordStrong(request.password())) {
            throw new ValidationException("Password does not meet strength requirements");
        }

        // Hash password
        String hashedPassword = passwordService.hashPassword(request.password());

        // Create user
        User user = User.builder()
                .tenantId(tenantId)
                .branchId(request.branchId())
                .username(request.username())
                .email(request.email())
                .hashedPassword(hashedPassword)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .userType(request.userType())
                .accessScope(request.accessScope())
                .build();

        user = userRepository.save(user);

        log.info("User registered successfully: {}", user.getUsername());

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getUserType()
        );
    }

    // helper function
    private AuthResult initiateForcePasswordReset(User user){
        // generate token
        String changeToken  = UUID.randomUUID().toString();

        // Store token in redis
        redisTemplate.opsForValue().set(
                AuthConstants.CHANGE_TOKEN_PREFIX + changeToken,
                user.getId().toString(),
                15, TimeUnit.MINUTES
        );
        // create a limited response
        LoginResponse limitedResponse = new LoginResponse(
                null, TOKEN_TYPE, 0,
                user.getId(), user.getUsername(),
                true, changeToken
        );
        // return the AuthResult
        return new AuthResult(limitedResponse, null);
    }

    private String validateResetToken(String changeToken) {
        String userId = redisTemplate.opsForValue().get(AuthConstants.CHANGE_TOKEN_PREFIX + changeToken);
        if (userId == null) {
            throw new UnauthorizedException("Invalid or expired reset token");
        }
        return userId;
    }

    private User loadUserFromToken(String userId) {
        return userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void updateUserPassword(User user, String newPassword) {
        if (!passwordService.isPasswordStrong(newPassword)) {
            throw new ValidationException("Password does not meet strength requirements");
        }
        user.setHashedPassword(passwordService.hashPassword(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    private void invalidateResetToken(String changeToken) {
        redisTemplate.delete(AuthConstants.CHANGE_TOKEN_PREFIX + changeToken);
    }

    private AuthResult generateAuthResult(User user) {
        School school = schoolRepository.findById(user.getTenantId())
                .orElseThrow(() -> new TenantNotFoundException("School not found"));

        String accessToken = jwtService.generateAccessToken(user, school);
        String refreshToken = refreshTokenService.createRefreshToken(user, school);

        log.info("Force reset completed for user: {} (tenant: {})", user.getId(), user.getTenantId());

        return new AuthResult(
                new LoginResponse(
                        accessToken, TOKEN_TYPE,
                        jwtService.getAccessTokenExpiration(), user.getId(),
                        user.getUsername(), false, null
                ),
                refreshToken
        );
    }
}
