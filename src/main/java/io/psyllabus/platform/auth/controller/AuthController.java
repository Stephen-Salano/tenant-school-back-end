package io.psyllabus.platform.auth.controller;

import io.psyllabus.platform.auth.dto.request.ForceResetRequest;
import io.psyllabus.platform.auth.dto.request.LoginRequest;
import io.psyllabus.platform.auth.dto.responses.AuthResult;
import io.psyllabus.platform.auth.dto.responses.ClaimsResponse;
import io.psyllabus.platform.auth.dto.responses.LoginResponse;
import io.psyllabus.platform.auth.dto.responses.RefreshResponse;
import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.auth.services.AuthService;
import io.psyllabus.platform.auth.services.ClaimsService;
import io.psyllabus.platform.auth.services.JwtService;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.psyllabus.platform.common.util.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.UUID;

/**
 * AuthController — login, token refresh, logout, and claims.
 *
 * Cookie strategy for refresh token:
 *   POST /login   → service returns AuthResult (accessToken + refreshJwt)
 *                   controller puts refreshJwt in Set-Cookie header (HttpOnly)
 *                   controller returns accessToken in JSON body
 *
 *   POST /refresh → browser auto-sends cookie; controller reads it via @CookieValue
 *                   calls authService.refresh() → returns new accessToken in JSON
 *
 *   POST /logout  → browser auto-sends cookie; controller reads it
 *                   calls authService.logout() → revokes in DB
 *                   controller clears cookie (maxAge=0)
 *
 * Why the controller handles cookies (not the service)?
 *   Services should not know about HTTP. Cookies are HTTP transport concerns.
 *   AuthService returns plain values; AuthController decides how to deliver them.
 */

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final ClaimsService claimsService;
    private final JwtService jwtService;
    private final Environment environment;

    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * POST /api/auth/login
     *
     * 1. Authenticates credentials, generates both tokens via AuthService
     * 2. Puts the refresh JWT into an HttpOnly cookie (browser stores it silently)
     * 3. Returns only the access token in the JSON body (frontend stores in memory)
     *
     * The refresh token never appears in the JSON response — JavaScript cannot read it.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthResult result = authService.login(request);

        if (result.refreshToken() != null){
            setRefreshCookie(response, result.refreshToken());
        }

        // Set refresh token as HttpOnly cookie — browser stores it, JS never sees it
        setRefreshCookie(response, result.refreshToken());

        return ResponseEntity.ok(ApiResponse.success(result.loginResponse(), "Login successful"));
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    /**
     * POST /api/auth/refresh
     *
     * Browser sends the HttpOnly cookie automatically — no body needed.
     * Returns a new access token in JSON.
     *
     * required=false: if cookie is missing (logged out, cleared) we return 401
     * rather than Spring throwing a MissingRequestCookieException (500).
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("No refresh token provided"));
        }

        UUID tenantId = TenantContext.getCurrentTenantId();
        String newAccessToken = authService.refresh(refreshToken, tenantId);

        RefreshResponse refreshResponse = RefreshResponse.of(
                newAccessToken,
                jwtService.getAccessTokenExpiration()
        );

        return ResponseEntity.ok(ApiResponse.success(refreshResponse, "Token refreshed"));
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    /**
     * POST /api/auth/logout
     *
     * 1. Reads refresh token from HttpOnly cookie
     * 2. Revokes it in DB (sets revoked=true on jti hash row)
     * 3. Clears the cookie (maxAge=0 tells browser to delete it)
     *
     * The access token will expire naturally on its own (short TTL).
     * For immediate access token invalidation, a token blocklist is needed (future sprint).
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            UUID tenantId = TenantContext.getCurrentTenantId();
            authService.logout(refreshToken, tenantId);
        }

        // Clear the cookie regardless — even if token was already invalid/missing
        clearRefreshCookie(response);

        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    // ── Claims ─────────────────────────────────────────────────────────────────

    /**
     * GET /api/auth/claims
     *
     * Called once after login to populate the dashboard.
     * Returns full user context: user, school, branch, subscription, features.
     * Cached in Redis — fast after first load.
     */
    @GetMapping("/claims")
    public ResponseEntity<ApiResponse<ClaimsResponse>> getClaims(
            @AuthenticationPrincipal User user
    ) {
        ClaimsResponse claimsResponse = claimsService.getFullClaims(user.getId());
        return ResponseEntity.ok(ApiResponse.success(claimsResponse, "Claims fetched successfully"));
    }

    @PostMapping("/reset-required")
    @Operation(summary = "Complete forced password reset",
        description = "Consumes the changeToken from login, sets new password, returns full session tokens."
    )
    public ResponseEntity<ApiResponse<LoginResponse>> completeForceReset(
            @Valid @RequestBody ForceResetRequest request,
            HttpServletResponse response
            ){
        AuthResult result = authService.completePasswordReset(
                request.changeToken(),
                request.newPassword()
        );
        if (result.refreshToken() != null){
            setRefreshCookie(response, result.refreshToken());
        }

        return ResponseEntity.ok(ApiResponse.success(result.loginResponse(), "Password reset successful"));
    }

    // ── Cookie helpers ─────────────────────────────────────────────────────────

    /**
     * Build and set the refresh token HttpOnly cookie.
     *
     * Attributes:
     *   HttpOnly  — JS cannot read it (XSS protection)
     *   Secure    — HTTPS only in production
     *   SameSite=Strict — cookie only sent on same-origin requests (CSRF protection)
     *   Path=/api/auth  — cookie scoped to auth endpoints only; browser won't send
     *                     it on /api/students, /api/staff etc. (least privilege)
     *   MaxAge    — mirrors JWT refresh token TTL from config
     */
    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(isProduction())           // HTTPS-only in prod
                .path("/api/auth")                // scoped — not sent to other API paths
                .maxAge(refreshTokenExpirationMs / 1000)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Clear the refresh token cookie on logout.
     * maxAge=0 instructs the browser to delete the cookie immediately.
     * Same attributes as setRefreshCookie so the browser matches the correct cookie.
     */
    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(isProduction())
                .path("/api/auth")
                .maxAge(0)                        // 0 = delete immediately
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private boolean isProduction() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("prod"));
    }
}
