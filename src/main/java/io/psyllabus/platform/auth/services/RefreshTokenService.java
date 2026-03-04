package io.psyllabus.platform.auth.services;

import io.psyllabus.platform.auth.entities.RefreshToken;
import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.school.entities.School;

import java.util.UUID;

/**
 * Manages the full lifecycle of DB-backed refresh tokens.
 *
 * Contract:
 *  - createRefreshToken  → generates raw refresh token, hashes + persists it, returns raw (shown once)
 *  - validateAndGet      → hash incoming → DB lookup → check validity → return entity
 *  - revoke              → single session logout
 *  - revokeAllForUser    → all-session logout (password change, suspension)
 *  - deleteExpired       → called by scheduler, not by request handlers
 */
public interface RefreshTokenService {
    /**
     * Create and persist a new refresh token for the given user.
     * Returns the RAW token string — must be sent to client immediately.
     * Only the hash is stored; the raw value cannot be recovered.
     */
    String createRefreshToken(User user, School school);

    /**
     * Validate an incoming raw refresh token.
     * Hashes it, looks up the DB record, checks revoked + expiry.
     *
     * @throws io.psyllabus.platform.common.exception.UnauthorizedException if invalid
     */
    RefreshToken validateAndGet(String refreshJwt, UUID tenantId);

    /**
     * Revoke a single refresh token (single-device logout).
     */
    void revoke(String refreshJwt, UUID tenantId);

    /**
     * Revoke ALL active refresh tokens for a user (all-device logout).
     * Called on: password change, account suspension.
     */
    void revokeAllForUser(UUID userId, UUID tenantId);

    /**
     * Delete expired tokens — called by the scheduler, not requests.
     */
    void deleteExpired();
}
