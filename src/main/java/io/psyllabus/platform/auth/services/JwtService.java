package io.psyllabus.platform.auth.services;

import io.jsonwebtoken.Claims;
import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.school.entities.School;
import io.psyllabus.platform.subscription.entity.Subscription;

import java.util.UUID;

/**
 * Service for JWT token generation and validation.
 * oken structure: tenant_id, user_id, user_type, subscription_plan, features[],
 *                   branch_id, access_scope, env, typ, aud, iss
 *
 */
public interface JwtService {

    /**
     * Generate JWT token with full multi-tenancy claims.
     *
     * @param user the authenticated user
     * @param school the user's school (tenant)
     * @param subscription the school's active subscription
     * @param type token type: "access" or "refresh"
     * @return signed JWT token string
     */
    String generateToken(User user, School school, Subscription subscription, String type);

    /**
     * Generate refresh token
     * @param user the authenticated user
     * @param school the user's school
     * @param subscription the school's active subscription
     * @return Signed access token
     */
    String generateAccessToken(User user, School school, Subscription subscription);

    /**
     * Generate the refresh token (wrapper for generateToken with type="refresh")
     *
     * @param user the authenticated user
     * @param school the user's school (tenant)
     * @param subscription the school's active subscription
     * @return signed refresh token
     */
    String generateRefreshToken(User user, School school, Subscription subscription);

    /**
     * Validate JWT token (signature, expiration, issuer, audience)
     * Used by JwtAuthenticationFilter to verify incoming requests
     *
     * @param token the JWT token to validate
     * @return true if valid, false otherwise
     */
    boolean validateToken(String token);

    /**
     * Validate token and verify username matches
     * Used when you have a User object and want to ensure token belongs to them.
     *
     * @param token the JWT token
     * @param username expected username from user
     * @return true if valid and username matches
     */
    boolean isTokenValid(String token, String username);

    /**
     * Extract all claims from token
     *
     * @param token the JWt token
     * @return Claims object with all token data
     * @throws RuntimeException if token parsing fails
     */
    Claims extractClaims(String token);

    /**
     * Extract tenant ID from token
     * Used by TenantFilter to set TenantContext
     *
     * @param token the JWT token
     * @return Tenant UUID
     */
    UUID extractTenantId(String token);

    /**
     * Extract user id from token
     *
     * @param token the JWT token
     * @return User UUID
     */
    UUID extractUserId(String token);

    /**
     * Extract username from token (subject claim)
     * @param token the JWT token
     * @return username String
     */
    String extractUsername(String token);

    /**
     * Check if token is expired
     *
     * @param token the JWT token
     * @return true if expired, false if still valid
     */
    boolean isTokenExpired(String token);

    /**
     * Get human-readable token metadata for logging/debugging
     * Format: "issuer=X, env=Y, type=Z, exp=timestamp
     * "
     * @param token the JWT token
     * @return Metadata string or "invalid_token" if parsing fails
     */
    String getTokenMetadata(String token);
}
