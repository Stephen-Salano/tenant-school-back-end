package io.psyllabus.platform.auth.services;

import io.psyllabus.platform.auth.dto.request.LoginRequest;
import io.psyllabus.platform.auth.dto.request.RegisterRequest;
import io.psyllabus.platform.auth.dto.responses.AuthResult;
import io.psyllabus.platform.auth.dto.responses.LoginResponse;
import io.psyllabus.platform.auth.dto.responses.UserResponse;

import java.util.UUID;

/**
 * Service for user authentication and registration.
 * Handles login, registration, and token generation.
 */
public interface AuthService {

    /**
     * Authenticate user and generate access + refresh token pair.
     *
     * Flow:
     * 1. Resolve tenant from TenantContext (set by TenantFilter)
     * 2. Find user by identifier within tenant
     * 3. Verify password
     * 4. Check account status
     * 5. Generate access token (JwtService)
     * 6. Generate refresh token (RefreshTokenService → JWT + jti hash stored in DB)
     * 7. Return AuthResult (loginResponse for JSON body + refreshToken for HttpOnly cookie)
     *
     * The controller splits AuthResult:
     *   loginResponse → JSON body
     *   refreshToken  → Set-Cookie header (HttpOnly, SameSite=Strict)
     *
     * @param request login credentials
     * @return AuthResult containing access token response + raw refresh JWT
     */
    AuthResult login(LoginRequest request);

    /**
     * Refresh an expired access token using a valid refresh token.
     *
     * Flow:
     * 1. Gate 1: JWT signature check (no DB) — proves we issued it
     * 2. Gate 2: DB revocation check — proves it hasn't been revoked
     * 3. Load User + School from accessToken claims
     * 4. Generate and return new access token
     *
     * @param refreshJwt the full refresh JWT (from HttpOnly cookie)
     * @param tenantId current tenant (from TenantContext)
     * @return new access token string
     */
    String refresh(String refreshJwt, UUID tenantId);

    /**
     * Logout — revoke the refresh token.
     *
     * Sets revoked=true on the DB row identified by the jti hash.
     * The access token remains valid until it naturally expires (short TTL by design).
     * For immediate access token invalidation, use a token blocklist (future sprint).
     *
     * @param refreshJwt the full refresh JWT (from HttpOnly cookie)
     * @param tenantId current tenant (from TenantContext)
     */
    void logout(String refreshJwt, UUID tenantId);

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

}
