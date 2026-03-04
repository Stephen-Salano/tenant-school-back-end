package io.psyllabus.platform.auth.services;

import io.psyllabus.platform.auth.entities.RefreshToken;
import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.auth.repository.RefreshTokenRepository;
import io.psyllabus.platform.common.exception.UnauthorizedException;
import io.psyllabus.platform.school.entities.School;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * RefreshToken lifecycle manager — Hybrid JWT + DB approach.
 *
 * Why hybrid?
 *   Pure opaque token (UUID): no provenance — anyone with DB access could fake a lookup.
 *   Pure JWT: no revocation — can't invalidate on staff termination or password change.
 *   Hybrid: best of both.
 *
 * How it works:
 *   1. JwtServiceImpl.generateRefreshToken() → signed JWT with jti claim inside
 *   2. We extract the jti from that JWT (extractJti)
 *   3. We SHA-256 hash the jti → store only the hash in DB
 *   4. Client receives the full JWT in an HttpOnly cookie
 *
 * Validation flow (two-gate):
 *   Gate 1 — Signature check (jwtService.validateToken):
 *     Proves WE issued this token. If tampered → rejected here. No DB hit.
 *   Gate 2 — Revocation check (DB lookup by jti hash):
 *     Proves it hasn't been revoked since issuance. DB hit only after Gate 1 passes.
 *
 * Hashing strategy:
 *   jti = UUID embedded in JWT (e.g. "550e8400-e29b-41d4-a716-446655440000")
 *   stored hash = SHA-256(jti) as hex string (64 chars)
 *   Why SHA-256 (not BCrypt)?
 *     jti is random (not user-chosen) — no rainbow table risk.
 *     SHA-256 is deterministic and fast — needed for lookups. BCrypt is overkill here.
 */

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RefreshTokenServiceImpl  implements  RefreshTokenService{

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiraionMs;

    @Override
    public String createRefreshToken(User user, School school) {
        //1 : Generate signed JWT refresh token - jti is embeded inside by JwtServiceImpl
        String refreshJwt = jwtService.generateRefreshToken(user, school);

        // 2: Extract the jti from the JWT - this is the unique ID we track in DB
        String jti = jwtService.extractJti(refreshJwt);

        // 3: Hashing the jti from the JWt - this is the unique id we track in the DB
        String jtiHash = sha256(jti);

        // Extract actual expiry from the JWT- no recomputation, no drift
        Date jwtExpiry = jwtService.extractClaims(refreshJwt).getExpiration();
        LocalDateTime expiresAt = jwtExpiry.toInstant()
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime();
        // 5: Persist the revocation record
        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(jtiHash)
                .userId(user.getId())
                .tenantId(school.getId())
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        log.info("Refresh token created for user: {} (tenant: {})", user.getId(), school.getId());

        // 6: Return the full JWT - sent to the client once via HttpOnly cookie (controllers job)
        return refreshJwt;
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken validateAndGet(String refreshJwt, UUID tenantId) {
        // ── Gate 1: Signature check ──────────────────────────────────────────────
        // Proves this token was signed by us. Tampered or forged tokens fail here.
        // No DB hit required for this gate.
        if (!jwtService.validateToken(refreshJwt)) {
            log.warn("Refresh token failed JWT signature/expiry validation");
            throw new UnauthorizedException("Invalid refresh token");
        }

        // ── Gate 2: Revocation check ─────────────────────────────────────────────
        // Proves the refresh token hasn't been revoked (logout, password change, suspension).
        String jti = jwtService.extractJti(refreshJwt);
        String jtiHash = sha256(jti);

        RefreshToken token = refreshTokenRepository.findByTokenHash(jtiHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        // Tenant isolation — token must belong to the active tenant
        if (!token.getTenantId().equals(tenantId)) {
            log.warn("Refresh token tenant mismatch: expected={}, actual={}", tenantId, token.getTenantId());
            throw new UnauthorizedException("Invalid refresh token");
        }

        if (token.isRevoked()) {
            log.warn("Attempted use of revoked refresh token for user: {}", token.getUserId());
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        // Secondary expiry guard — JWT expiry already checked by Gate 1,
        // but DB expiresAt catches edge cases (e.g. admin-forced expiry, clock adjustments)
        if (token.isExpired()) {
            log.info("Expired refresh token used for user: {}", token.getUserId());
            throw new UnauthorizedException("Refresh token has expired");
        }

        return token;
    }

    @Override
    public void revoke(String refreshJwt, UUID tenantId) {
        try {
            String jti = jwtService.extractJti(refreshJwt);
            String jtiHash = sha256(jti);

            refreshTokenRepository.findByTokenHash(jtiHash).ifPresent(token -> {
                if (token.getTenantId().equals(tenantId)) {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                    log.info("Refresh token revoked for user: {}", token.getUserId());
                }
            });
        } catch (Exception e) {
            // Silent fail — a JWT that can't be parsed is already effectively invalid.
            // No active session to revoke, so this is safe to ignore.
            log.warn("Could not revoke refresh token (may already be invalid): {}", e.getMessage());
        }
    }

    @Override
    public void revokeAllForUser(UUID userId, UUID tenantId) {
        List<RefreshToken> activeTokens = refreshTokenRepository
                .findByUserIdAndTenantIdAndRevokedFalse(userId, tenantId);

        if (activeTokens.isEmpty()) {
            return;
        }

        activeTokens.forEach(t -> t.setRevoked(true));
        refreshTokenRepository.saveAll(activeTokens);

        log.info("Revoked {} refresh token(s) for user: {} (tenant: {})",
                activeTokens.size(), userId, tenantId);
    }

    /**
     * Nightly cleanup — 2am Nairobi time (EAT = UTC+3, cron at 11pm UTC).
     * Deletes rows where expires_at < NOW(). Short transaction — PGBouncer safe.
     */
    @Scheduled(cron = "0 0 23 * * *")
    @Override
    public void deleteExpired() {
        int deleted = refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Refresh token cleanup: deleted {} expired token(s)", deleted);
    }

    // ============== Helper Methods =====================================
    /**
     * SHA-256 hash of input string → 64-char lowercase hex.
     * Deterministic, fast — correct for random jti values.
     * Java 17+ HexFormat — no external dependency needed.
     */
    private String sha256(String input){
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e){
            // SHA-256 is guaranteed by the JVM spec - this will probably never throw
            throw new IllegalArgumentException("SHA-256 not available", e);
        }
    }
}
