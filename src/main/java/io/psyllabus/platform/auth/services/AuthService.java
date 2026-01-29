package io.psyllabus.platform.auth.services;

import io.psyllabus.platform.auth.dto.request.LoginRequest;
import io.psyllabus.platform.auth.dto.request.RegisterRequest;
import io.psyllabus.platform.auth.dto.responses.LoginResponse;
import io.psyllabus.platform.auth.dto.responses.UserResponse;

import java.util.UUID;

/**
 * Service for user authentication and registration.
 * Handles login, registration, and token generation.
 */
public interface AuthService {

    /**
     * Authenticate user and generate JWT token.
     *
     * Flow:
     * 1. Find user by username/email within tenant
     * 2. Verify password using PasswordService
     * 3. Load School and Subscription for tenant
     * 4. Generate JWT using JwtService
     * 5. Return LoginResponse with token + user info
     *
     * @param request login credentials (username/email + password)
     * @return LoginResponse containing JWT token and user details
     * @throws io.psyllabus.platform.common.exception.UnauthorizedException if credentials invalid
     * @throws io.psyllabus.platform.common.exception.TenantNotFoundException if tenant not found
     */
    LoginResponse login(LoginRequest request);

    /**
     * Register a new user within a tenant.
     *
     * Flow:
     * 1. Validate unique username/email per tenant
     * 2. Validate password strength
     * 3. Hash password using PasswordService
     * 4. Create User entity
     * 5. Save to database
     * 6. Return UserResponse (no token - requires separate login)
     *
     * @param request user registration details
     * @return UserResponse with created user details
     * @throws io.psyllabus.platform.common.exception.DuplicateResourceException if username/email exists
     * @throws io.psyllabus.platform.common.exception.ValidationException if password weak
     */
    UserResponse register(RegisterRequest request);

    /**
     * Verify if a user with given credentials exists.
     * Used for pre-login checks.
     *
     * @param username username or email
     * @param tenantId tenant ID
     * @return true if user exists, false otherwise
     */
    boolean userExists(String username, UUID tenantId);
}
