package io.psyllabus.platform.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

/**
 * Enhanced Security Headers Configuration.
 * Combines environment-aware flexibility with strict API data protection.
 */
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class SecurityHeadersConfig extends OncePerRequestFilter {

    private final Environment environment;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // --- 1. BASE SECURITY (Always Active) ---

        // Prevents MIME-type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Disables legacy/buggy XSS auditor in favor of CSP
        response.setHeader("X-XSS-Protection", "0");

        // Limits browser features (Privacy focused)
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");

        // --- 2. API DATA PROTECTION (Adapted from v2) ---

        // Ensure sensitive API data is never stored in browser cache (K-DPA compliance)
        if (request.getRequestURI().startsWith("/api/")) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            response.setHeader("Pragma", "no-cache");
        }

        // --- 3. ENVIRONMENT-AWARE HEADERS (Your Original Logic) ---

        if (isProduction()) {
            // Maximum Lockdown
            response.setHeader("Referrer-Policy", "no-referrer");
            response.setHeader("X-Frame-Options", "DENY");
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            response.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self'; object-src 'none';");
        } else {
            // Relaxed for Dev Tools (Swagger, H2 Console)
            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            response.setHeader("X-Frame-Options", "SAMEORIGIN");
            response.setHeader("Content-Security-Policy",
                    "default-src 'self'; " +
                            "script-src 'self' 'unsafe-inline'; " +
                            "style-src 'self' 'unsafe-inline'; " +
                            "img-src 'self' data:; " +
                            "frame-ancestors 'self';");
        }

        filterChain.doFilter(request, response);
    }

    private boolean isProduction() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equalsIgnoreCase("prod"));
    }
}