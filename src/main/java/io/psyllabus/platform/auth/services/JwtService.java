package io.psyllabus.platform.auth.services;

import io.jsonwebtoken.Claims;
import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.auth.enums.UserType;
import io.psyllabus.platform.platformadmin.entities.PlatformAdmin;
import io.psyllabus.platform.school.entities.School;

import java.util.UUID;

/**
 * Service for JWT access and refresh token generation and validation.
 * Token structure: tenant_id (school users only), user_id, user_type, env, typ, aud, iss, jti.
 *
 * <p>Two identity paths exist:</p>
 * <ul>
 *   <li>School users ({@link User}) — always include a {@code tenant_id} claim.</li>
 *   <li>Platform admins ({@link PlatformAdmin}) — no {@code tenant_id}; identified solely
 *       by {@code user_type = PLATFORM_ADMIN}. Use the overloaded methods below.</li>
 * </ul>
 */
public interface JwtService {

    /**
     * Generate a JWT for a school-level user.
     *
     * <p>If {@code school} is {@code null} the {@code tenant_id} claim is omitted.
     * This handles the rare case where a {@link User} record exists before a school
     * record is fully committed; callers should always pass a non-null school for
     * normal authentication flows.</p>
     *
     * @param user   the authenticated user
     * @param school the user's school (tenant); may be {@code null} only for edge-case flows
     * @param type   token type — {@code "access"} or {@code "refresh"}
     * @return signed JWT token string
     */
    String generateToken(User user, School school, String type);

    /**
     * Generate an access token for a school-level user.
     *
     * @param user   the authenticated user
     * @param school the user's school
     * @return signed access JWT
     */
    String generateAccessToken(User user, School school);

    /**
     * Generate a refresh token for a school-level user.
     * The {@code jti} embedded in this token is what {@code RefreshTokenService}
     * hashes and stores for revocation tracking.
     *
     * @param user   the authenticated user
     * @param school the user's school
     * @return signed refresh JWT
     */
    String generateRefreshToken(User user, School school);

    /**
     * Generate a JWT for a platform-level administrator.
     *
     * <p>No {@code tenant_id} claim is set — platform admins operate across all
     * tenants and have no school affiliation. {@code user_type} is always
     * {@code PLATFORM_ADMIN}. {@code TenantFilter} skips tenant resolution for
     * tokens carrying this type; {@code JwtAuthFilter} loads the principal from
     * the {@code platform_admins} table instead of {@code users}.</p>
     *
     * @param admin the authenticated platform admin
     * @param type  token type — {@code "access"} or {@code "refresh"}
     * @return signed JWT token string
     */
    String generatePlatformToken(PlatformAdmin admin, String type);

    /**
     * Generate an access token for a platform-level administrator.
     *
     * @param admin the authenticated platform admin
     * @return signed access JWT
     */
    String generatePlatformAccessToken(PlatformAdmin admin);

    /**
     * Validate a JWT token — checks signature, expiration, issuer, and audience.
     * Used by {@code JwtAuthFilter} on every incoming request.
     *
     * @param token the JWT token string
     * @return {@code true} if all validation checks pass, {@code false} otherwise
     */
    boolean validateToken(String token);

    /**
     * Validate a JWT and confirm the {@code sub} claim matches the expected username.
     * Used when a {@link User} or {@link PlatformAdmin} object is already loaded.
     *
     * @param token    the JWT token
     * @param username expected username from the loaded principal
     * @return {@code true} if valid and username matches
     */
    boolean isTokenValid(String token, String username);

    /**
     * Extract all claims from a token. Verifies signature before returning.
     *
     * @param token the JWT token
     * @return parsed {@link Claims}
     * @throws io.psyllabus.platform.common.exception.UnauthorizedException if parsing fails
     */
    Claims extractClaims(String token);

    /**
     * Extract the {@code jti} (JWT ID) claim.
     *
     * <p>Used by {@code RefreshTokenService} after calling {@link #generateRefreshToken}
     * — it extracts the jti, SHA-256 hashes it, and stores the hash in DB for
     * revocation tracking.</p>
     *
     * @param token the JWT token
     * @return jti string (UUID format)
     * @throws IllegalArgumentException if the token has no jti claim
     */
    String extractJti(String token);

    /**
     * Extract the {@code tenant_id} claim.
     *
     * <p><strong>Important:</strong> this claim is absent from platform admin tokens.
     * Callers should check {@link #extractUserType} first if there is any chance
     * the token belongs to a platform admin.</p>
     *
     * @param token the JWT token
     * @return tenant UUID
     */
    UUID extractTenantId(String token);

    /**
     * Extract the {@code user_id} claim.
     *
     * @param token the JWT token
     * @return user UUID (works for both school users and platform admins)
     */
    UUID extractUserId(String token);

    /**
     * Extract the username from the {@code sub} claim.
     *
     * @param token the JWT token
     * @return username string
     */
    String extractUsername(String token);

    /**
     * Extract the {@code user_type} claim.
     *
     * <p>Used by {@code JwtAuthFilter} to decide which identity table to query:
     * {@code platform_admins} for {@link UserType#PLATFORM_ADMIN}, {@code users}
     * for everyone else. Returns {@code null} if the claim is absent (e.g. old
     * tokens issued before this claim was added).</p>
     *
     * @param token the JWT token
     * @return the {@link UserType}, or {@code null} if the claim is missing or unrecognisable
     */
    UserType extractUserType(String token);

    /**
     * Check if a token has passed its expiration date.
     *
     * @param token the JWT token
     * @return {@code true} if expired or unparseable, {@code false} if still valid
     */
    boolean isTokenExpired(String token);

    /**
     * Return human-readable token metadata for logging and debugging.
     * Format: {@code "issuer=X, env=Y, type=Z, exp=timestamp"}.
     *
     * @param token the JWT token
     * @return metadata string, or {@code "invalid_token"} if parsing fails
     */
    String getTokenMetadata(String token);

    /**
     * Return the configured access token expiration time in milliseconds.
     *
     * @return expiration duration in ms
     */
    long getAccessTokenExpiration();
}
