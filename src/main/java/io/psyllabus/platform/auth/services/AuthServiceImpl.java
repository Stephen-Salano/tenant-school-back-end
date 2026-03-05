package io.psyllabus.platform.auth.services;

import io.psyllabus.platform.auth.dto.request.LoginRequest;
import io.psyllabus.platform.auth.dto.request.RegisterRequest;
import io.psyllabus.platform.auth.dto.responses.AuthResult;
import io.psyllabus.platform.auth.dto.responses.LoginResponse;
import io.psyllabus.platform.auth.dto.responses.UserResponse;
import io.psyllabus.platform.auth.entities.RefreshToken;
import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.auth.repository.UserRepository;
import io.psyllabus.platform.common.exception.*;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.school.entities.School;
import io.psyllabus.platform.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

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

        // Checking user status (to avoid double login)
        if(!user.isEnabled()){
            log.warn("Login attempt for disabled user: {}", request.username());
            throw new UnauthorizedException("Account is disabled");
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
                "Bearer",
                jwtService.getAccessTokenExpiration(),
                user.getId(),
                user.getUsername()
        );
     return new AuthResult(loginResponse, refreshToken);
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
    @CacheEvict(value = {"userProfile", "userClaims"}, key = "#refreshJwt", condition = "false")
    // TODO: we can't key cache eviction by userId here since we only have the JWT.
    // Full cache eviction on logout is handled separately if needed (future sprint).
    public void logout(String refreshJwt, UUID tenantId) {
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
}
