package io.psyllabus.platform.auth.services;

import io.psyllabus.platform.auth.dto.request.LoginRequest;
import io.psyllabus.platform.auth.dto.request.RegisterRequest;
import io.psyllabus.platform.auth.dto.responses.LoginResponse;
import io.psyllabus.platform.auth.dto.responses.UserResponse;
import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.auth.repository.UserRepository;
import io.psyllabus.platform.common.exception.DuplicateResourceException;
import io.psyllabus.platform.common.exception.TenantNotFoundException;
import io.psyllabus.platform.common.exception.UnauthorizedException;
import io.psyllabus.platform.common.exception.ValidationException;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.school.entities.School;
import io.psyllabus.platform.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of AuthService.
 * Handles user authentication and registration with multi-tenancy support.
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

    @Override
    public LoginResponse login(LoginRequest request) {
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

        // Step 6: Generate Minimal JWT token (Identity Only)
        String token = jwtService.generateAccessToken(user, school);

        log.info("Login successful for user: {} (tenant: {})", user.getUsername(), school.getName());

        // Step 7: Return Minimal Login Response
        return new LoginResponse(
                token,
                "Bearer",
                jwtService.getAccessTokenExpiration(),
                user.getId(),
                user.getUsername()
        );
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

    @Override
    public boolean userExists(String username, UUID tenantId) {
        return userRepository.findByIdentifierAndTenantId(username, tenantId).isPresent();
    }
}
