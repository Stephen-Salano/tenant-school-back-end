package io.psyllabus.platform.platformadmin.controller;

import io.psyllabus.platform.auth.dto.request.LoginRequest;
import io.psyllabus.platform.auth.dto.responses.AuthResult;
import io.psyllabus.platform.auth.dto.responses.LoginResponse;
import io.psyllabus.platform.auth.dto.responses.RefreshResponse;
import io.psyllabus.platform.auth.services.AuthService;
import io.psyllabus.platform.auth.services.JwtService;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * PlatformAuthController — login, token refresh, and logout for platform admins.
 * Scoped to /api/platform/auth/**.
 */
@RestController
@RequestMapping("/api/platform/auth")
@RequiredArgsConstructor
@Tag(name = "Platform Auth", description = "Platform admin authentication")
public class PlatformAuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final Environment environment;

    private static final String REFRESH_COOKIE_NAME = "platform_refresh_token";

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    @PostMapping("/login")
    @Operation(summary = "Platform admin login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthResult result = authService.loginPlatformAdmin(request);

        if (result.refreshToken() != null) {
            setRefreshCookie(response, result.refreshToken());
        }

        return ResponseEntity.ok(ApiResponse.success(result.loginResponse(), "Login successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh platform admin access token")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("No refresh token provided"));
        }

        String newAccessToken = authService.refreshPlatformAdmin(refreshToken);

        RefreshResponse refreshResponse = RefreshResponse.of(
                newAccessToken,
                jwtService.getAccessTokenExpiration()
        );

        return ResponseEntity.ok(ApiResponse.success(refreshResponse, "Token refreshed"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Platform admin logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logoutPlatformAdmin(refreshToken);
        }

        clearRefreshCookie(response);

        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(isProduction())
                .path("/api/platform/auth")
                .maxAge(refreshTokenExpirationMs / 1000)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(isProduction())
                .path("/api/platform/auth")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private boolean isProduction() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("prod"));
    }
}
