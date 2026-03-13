package io.psyllabus.platform.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.psyllabus.platform.common.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * First gate in the filter chain, protecting platform-level endpoints.
 *
 * <h2>Why This Exists</h2>
 * <p>The endpoints {@code /api/schools/onboard} and {@code /api/platform/**} are critical
 * platform-level resources. They must never be reachable without the platform API key,
 * even before tenant resolution or JWT validation occurs. This filter provides
 * defense-in-depth, ensuring that even if a later security layer is misconfigured,
 * this primary key check still blocks unauthorized access.</p>
 *
 * <h2>Order of Execution</h2>
 * <p>This filter runs <strong>before</strong> {@code TenantFilter} and {@code JwtAuthFilter}.
 * It is registered via {@code SecurityConfig.addFilterBefore(this, TenantFilter.class)}.</p>
 *
 * <h2>Protected Paths</h2>
 * <ul>
 *   <li>{@code POST /api/schools/onboard} - Closes public school creation (addresses Issue #35).</li>
 *   <li>{@code /api/platform/**} - Secures all current and future platform-admin endpoints.</li>
 * </ul>
 *
 * <h2>Configuration in Development</h2>
 * <p>In development, the key defaults to {@code "dev-platform-key-change-in-prod"} via a
 * fallback in {@code application.yml}. Use this value in your Postman environment for the
 * {@code X-Platform-Key} header.</p>
 *
 * <h2>Configuration in Production</h2>
 * <p>The {@code application-prod.yml} has no fallback. The application will fail to start
 * if the {@code PLATFORM_API_KEY} environment variable is missing. This is intentional,
 * as a missing key in production is a critical misconfiguration, not a soft failure.</p>
 *
 * <h2>Security Note: Timing-Safe Comparison</h2>
 * <p>The filter uses a constant-time comparison functionally equivalent to
 * {@code MessageDigest.isEqual()} to prevent timing attacks. This ensures that an
 * attacker cannot guess the key byte-by-byte by measuring response time variations.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlatformKeyFilter extends OncePerRequestFilter {

    private static final String PLATFORM_KEY_HEADER = "X-Platform-Key";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final List<String> PROTECTED_PATHS = List.of(
            "/api/schools/onboard",
            "/api/platform/**"
    );

    @Value("${platform.api-key}")
    private String expectedApiKey;

    private final ObjectMapper objectMapper;

    /**
     * Core filter logic for platform-level path protection.
     *
     * <p>Acts as the first gate in the filter chain. Requests to protected paths
     * must carry the correct {@code X-Platform-Key} header or they are rejected
     * immediately with HTTP 403 before tenant resolution or JWT validation runs.</p>
     *
     * @param request  the incoming HTTP request
     * @param response the HTTP response — written to directly on rejection
     * @param filterChain    the filter chain; only invoked if the key is valid
     * @throws ServletException if the filter chain throws
     * @throws IOException      if writing the 403 response body fails
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String providedKey = request.getHeader(PLATFORM_KEY_HEADER);

        if (providedKey == null || providedKey.isBlank()){
            log.warn("Platform key is missing on protected path: {} {}",
                    request.getMethod(), request.getRequestURI());
            rejectRequest(response, "Missing X-Platform-Key Header");
            return;
        }
        // Constant-time comparison - prevents timing side-channel attacks
        if (!timingSafeEquals(expectedApiKey, providedKey)){
            log.warn("Invalid platform key attempt on: {} {} from IP: {}",
                request.getMethod(), request.getRequestURI(), getClientIp(request)
            );
            rejectRequest(response, "Invalid platform Key");
            return;
        }
        log.debug("Platform key validated for: {} {}", request.getMethod(), request.getRequestURI());
        filterChain.doFilter(request, response);
    }

    /**
     * Determines whether the filter should be applied to a given request.
     * <p>
     * This method returns {@code true} for paths that do <strong>not</strong> require
     * platform key validation. The filter is designed to only run on the specified
     * {@code PROTECTED_PATHS}. All other routes, such as school-level endpoints and
     * authentication routes, will pass through untouched, bypassing the
     * {@code doFilterInternal} logic entirely.
     * </p>
     *
     * @param request the current HTTP request
     * @return {@code false} if the request path matches a protected pattern, {@code true} otherwise.
     * @throws ServletException if an error occurs during processing.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        boolean isProtected = PROTECTED_PATHS.stream()
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
        return !isProtected;
    }

    /**
     * Writes a JSON 403 Forbidden response directly to the {@link HttpServletResponse}.
     * <p>
     * This method bypasses Spring's normal exception handling because it operates
     * early in the filter chain, before the Spring MVC dispatcher servlet has
     * processed the request.
     * </p>
     *
     * @param response the {@link HttpServletResponse} to write the error to.
     * @param message  the error message to include in the JSON response body.
     * @throws IOException if an error occurs while writing to the response writer.
     */
    private void rejectRequest(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> body = ApiResponse.error(message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    /**
     * Performs a constant-time string comparison to mitigate timing attacks.
     * <p>
     * Standard string comparison (e.g., {@code String.equals()}) short-circuits on
     * the first mismatch, creating a small but measurable time difference. An attacker
     * could exploit this to guess a secret one character at a time. This implementation
     * avoids that by always comparing all bytes, ensuring the execution time is not
     * correlated with the number of correct leading characters.
     * </p>
     *
     * @param a the first string (e.g., the expected API key).
     * @param b the second string (e.g., the provided API key).
     * @return {@code true} if the strings are identical, {@code false} otherwise.
     */
    private boolean timingSafeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] aBytes = a.getBytes();
        byte[] bBytes = b.getBytes();

        if (aBytes.length != bBytes.length) return false;

        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }

    /**
     * Provides a best-effort extraction of the client's IP address.
     * <p>
     * It first checks for the {@code X-Forwarded-For} header, which is commonly
     * added by reverse proxies like Nginx. If that header is not present, it
     * falls back to the standard {@link HttpServletRequest#getRemoteAddr()}.
     * </p>
     *
     * @param request the incoming {@link HttpServletRequest}.
     * @return the determined client IP address as a string.
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
