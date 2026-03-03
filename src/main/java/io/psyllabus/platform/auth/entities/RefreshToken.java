package io.psyllabus.platform.auth.entities;

import io.psyllabus.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DB-backed refresh token store.
 *
 * We never store the raw token — only its SHA-256 hash.
 * Think of it like a password: if someone reads the DB, the raw token is useless.
 *
 * Lifecycle:
 *   Login → create row (hash stored, raw token sent to client ONCE)
 *   /refresh → hash incoming token → look up here → check not revoked + not expired
 *   /logout → set revoked = true
 *   Password change → revokeAll for that userId
 *   Scheduler → delete rows where expires_at < NOW()
 *
 * Extends Auditable: gives us createdAt (= issued time) and createdBy (= userId)
 * for free — satisfies K-DPA audit trail for session creation.
 */

@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_refresh_token_user", columnList = "user_id, tenant_id"),
        @Index(name = "idx_refresh_token_expiry", columnList = "expires_at") // for cleanup queries
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * SHA-256 hash of the raw token.
     * Raw token is a UUID string — we hash it before persisting.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private Boolean revoked = false;

    /**
     * Optional: browser/device hint for "active sessions" page later.
     * e.g. "Chrome on Android", "Postman"
     */
    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    // ── Convenience checks ─────────────────────────────────────────────────────

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * A token is only usable if it is both not revoked AND not past its expiry.
     * Checking both here keeps the validation logic out of the service.
     */
    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
