package io.psyllabus.platform.auth.services;

import io.psyllabus.platform.auth.dto.request.LoginRequest;
import io.psyllabus.platform.auth.dto.request.RegisterRequest;
import io.psyllabus.platform.auth.dto.responses.LoginResponse;
import io.psyllabus.platform.auth.dto.responses.UserResponse;
import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.auth.repository.UserRepository;
import io.psyllabus.platform.common.exception.TenantNotFoundException;
import io.psyllabus.platform.common.exception.UnauthorizedException;
import io.psyllabus.platform.school.entities.School;
import io.psyllabus.platform.school.repository.SchoolRepository;
import io.psyllabus.platform.subscription.entity.Subscription;
import io.psyllabus.platform.subscription.repository.SubscriptionRepository;
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
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordService passwordService;
    private final JwtServiceImpl jwtService;

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for username: {}", request.username());

        // Step 1: Extract tenant from the username if it contains subdomain
        // format: username@school-a or just username
        String [] parts = request.username().split("@");
        String identifier = parts[0]; // username / email / admission number
        String subdomainPrefix = parts.length > 1 ? parts[1] : null;


        // Step 2: Find school (tenant)
        School school;
        if (subdomainPrefix != null){
            school = schoolRepository.findBySubdomainPrefix(subdomainPrefix)
                    .orElseThrow(() -> new TenantNotFoundException("School not found: " + subdomainPrefix));
        } else {
            // If no subdomain, we need tenant_id from request or context
            throw new TenantNotFoundException("Tenant must be specified (use 'username@subdomain' format)");
        }

        log.debug("Login for tenant: {} ({})", school.getName(), school.getId());

        // Step 3: Find user by identifier (username, email, or admission number)
        User user = userRepository.findByIdentifierAndTenantId(identifier, school.getId())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        // Step 5: Check user status
        if (!user.isEnabled()){
            log.warn("Login attempt for disabled user: {}", identifier);
            throw new UnauthorizedException("Account is disabled");
        }

        // Step 6: Load subscription
        Subscription subscription = subscriptionRepository.findByTenantIdAndIsActiveTrue(school.getId())
                .orElseThrow(() -> new TenantNotFoundException("No active subscription found"));

        if (!subscription.isValid()){
            log.warn("Login attempt for school with expired subscription: {}", school.getName());
            throw new UnauthorizedException("School subscription has expired");
        }

        // Step 7: Generate JWT token
        String token = jwtService.generateAccessToken(user, school, subscription);

        log.info("Login successful for user: {} (tenant: {})", user.getUsername(), school.getName());

        // Step 8: Return Login Response
        return new LoginResponse(
                token, "Bearer",
                jwtService.getAccessTokenExpiration(),
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getUserType(),
                school.getId(),
                user.getBranchId(),
                user.getAccessScope(),
                subscription.getPlanType(),
                subscription.getFeatures()
        );
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        return null;
    }

    @Override
    public boolean userExists(String username, UUID tenantId) {
        return false;
    }
}
