package io.psyllabus.platform.auth.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.school.entities.School;
import io.psyllabus.platform.subscription.entity.Subscription;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Implementation of JwtService using JJWT library.
 * Generates JWT tokens with full multi-tenancy and subscription claims.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Getter
public class JwtServiceImpl implements JwtService {

    private final Environment environment;

    @Value("${jwt.secret-key}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.application.frontend-url}")
    private String audience;

    private static final String CLAIM_TENANT_ID = "tenant_id";
    private static final String CLAIM_USER_ID = "user_id";
    private static final String CLAIM_USER_TYPE = "user_type";
    private static final String CLAIM_SUBSCRIPTION_PLAN = "subscription_plan";
    private static final String CLAIM_FEATURES = "features";
    private static final String CLAIM_BRANCH_ID = "branch_id";
    private static final String CLAIM_ACCESS_SCOPE = "access_scope";
    private static final String ENVIRONMENT_CLAIM = "env";
    private static final String TOKEN_TYPE_CLAIM = "typ";


    @Override
    public String generateToken(User user, School school, Subscription subscription, String type) {
        Instant now = Instant.now();

        // If token_type = "access" => use access_token_ttl else use refresh_token_ttl
        Instant expiry = now.plus(type.equals("access") ? accessTokenExpiration : refreshTokenExpiration, ChronoUnit.MILLIS);

        Map<String, Object> claims = new HashMap<>();

        // Standard metadata
        claims.put(TOKEN_TYPE_CLAIM, type);
        claims.put(ENVIRONMENT_CLAIM, getCurrentEnvironment());
        claims.put("aud", Collections.singleton(audience));
        
        
        // Business claims for multi-tenancy and feature gating
        claims.put(CLAIM_USER_ID, user.getId().toString());
        claims.put(CLAIM_TENANT_ID, school.getId().toString());
        claims.put(CLAIM_USER_TYPE, user.getUserType().name());
        claims.put(CLAIM_ACCESS_SCOPE, user.getAccessScope().name());
        claims.put(CLAIM_SUBSCRIPTION_PLAN, subscription.getPlanType().name());
        claims.put(CLAIM_FEATURES, subscription.getFeatures());

        if (user.getBranchId() != null){
            claims.put(CLAIM_BRANCH_ID, user.getBranchId().toString());
        }

        String token = Jwts.builder()
                .claims(claims)
                .subject(user.getUsername())
                .issuer(getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey())
                .compact();

        log.debug("JWT token generated: user={}, type={}, expires={}", user.getUsername(), type, expiry);
        return token;
    }

    @Override
    public String generateAccessToken(User user, School school, Subscription subscription) {
        log.debug("Generating access token for user: {}", user.getUsername());
        return generateToken(user, school, subscription, "access");
    }

    @Override
    public String generateRefreshToken(User user, School school, Subscription subscription) {
        log.debug("Generating refresh token for user: {}", user.getUsername());
        return generateToken(user, school, subscription, "refresh");
    }

    @Override
    public boolean validateToken(String token) {
        try{
            // parse and verify signature
            Claims claims = extractClaims(token);

            // Check expiration
            if (isTokenExpired(token)){
                log.warn("Token expired");
                return false;
            }

            // Validate issuer
            if (!validateIssuer(token)){
                log.warn("Invalid issuer");
                return false;
            }

            // Validate audience
            if (!validateAudience(token)){
                log.warn("Invalid audience");
                return false;
            }

            log.debug("Token validation successful");
            return true;
        } catch (Exception e){
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isTokenValid(String token, String username) {
        try{
            String extractedUsername = extractUsername(token);
            boolean valid = extractedUsername.equals(username) && validateToken(token);

            log.debug("Token validity for {}: {}", username, valid);
            return valid;
        } catch (Exception e){
            log.warn("Token validation ith username failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e){
            log.error("Token parsing failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse JWT token", e);
        }
    }

    @Override
    public UUID extractTenantId(String token) {
        Claims claims = extractClaims(token);
        String tenantIdStr = claims.get(CLAIM_TENANT_ID, String.class);
        return UUID.fromString(tenantIdStr);
    }

    @Override
    public UUID extractUserId(String token) {
        Claims claims = extractClaims(token);
        String userIdStr = claims.get(CLAIM_USER_ID, String.class);
        return UUID.fromString(userIdStr);
    }

    @Override
    public String extractUsername(String token) {
        Claims claims = extractClaims(token);
        return claims.getSubject();
    }

    @Override
    public boolean isTokenExpired(String token) {
        try{
            Claims claims = extractClaims(token);
            boolean expired = claims.getExpiration().before(new Date());

            if (expired){
                log.debug("Token expired at {}", claims.getExpiration());
            }

            return expired;
        } catch (Exception e){
            log.warn("Failed to check token expiration: {}", e.getMessage());
            return true; // We treat invalid tokens same as expired ones
        }
    }

    @Override
    public String getTokenMetadata(String token) {
        try{
            Claims claims = extractClaims(token);
            return String.format("issuer=%s, env=%s, type=%s, exp=%s",
                    claims.getIssuer(), claims.get(ENVIRONMENT_CLAIM),
                    claims.get(TOKEN_TYPE_CLAIM), claims.getExpiration()
                    );
        } catch (Exception e){
            return "invalid_token";
        }
    }

    // ==================== helper methods =======================================

    /**
     * Get signing key for JWT signature.
     * Decodes Base64-encoded secret from configuration.
     */
    private SecretKey getSigningKey() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            log.error("Signing key error: {}", e.getMessage(), e);
            throw new RuntimeException("Invalid JWT signing key", e);
        }
    }

    /**
     * Get issuer string: applicationName_environment
     * Example: "SchoolMgmt_prod"
     */
    private String getIssuer() {
        return applicationName + "_" + getCurrentEnvironment();
    }

    /**
     * Get current Spring profile (dev, test, staging, prod).
     * Defaults to "default" if no recognized profile is active.
     */
    private String getCurrentEnvironment() {
        return Arrays.stream(environment.getActiveProfiles())
                .filter(p -> List.of("dev", "staging", "prod", "test").contains(p))
                .findFirst()
                .orElse("default");
    }

    /**
     * Validate token issuer matches expected application + environment.
     */
    private boolean validateIssuer(String token) {
        try {
            String expected = getIssuer();
            Claims claims = extractClaims(token);
            String actual = claims.getIssuer();

            boolean valid = expected.equals(actual);
            if (!valid) {
                log.warn("Issuer mismatch: expected={}, actual={}", expected, actual);
            }

            return valid;
        } catch (Exception e) {
            log.warn("Issuer validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate token audience contains expected frontend URL.
     */
    private boolean validateAudience(String token) {
        try {
            Claims claims = extractClaims(token);
            Set<String> audienceSet = claims.getAudience();

            boolean valid = audienceSet != null && audienceSet.contains(audience);
            if (!valid) {
                log.warn("Audience mismatch: expected contains={}, actual={}", audience, audienceSet);
            }

            return valid;
        } catch (Exception e) {
            log.warn("Audience validation failed: {}", e.getMessage());
            return false;
        }
    }
}
