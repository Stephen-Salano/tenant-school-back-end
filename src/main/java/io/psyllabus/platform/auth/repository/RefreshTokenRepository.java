package io.psyllabus.platform.auth.repository;

import io.psyllabus.platform.auth.entities.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Primary lookup — used by /refresh and /logout.
     * We always hash the incoming raw token first, then call this.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Used by revokeAll — called on password change or account suspension.
     * Only returns active (non-revoked) tokens to avoid unnecessary updates.
     */
    List<RefreshToken> findByUserIdAndTenantIdAndRevokedFalse(UUID userId, UUID tenantId);

    /**
     * Bulk delete for the nightly cleanup scheduler.
     * Uses @Modifying + JPQL so we don't load entities into memory first.
     * Short transaction — works well with PGBouncer.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :cutoff")
    int deleteByExpiresAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
