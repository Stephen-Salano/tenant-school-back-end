package io.psyllabus.platform.common.filter;

import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.auth.repository.UserRepository;
import io.psyllabus.platform.auth.services.JwtService;
import io.psyllabus.platform.common.util.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Validates the Bearer JWT and sets the {@code SecurityContext} principal.
 *
 * <p>Runs second in the filter chain (after {@code TenantFilter}). Operates on
 * two distinct identity paths:</p>
 *
 * <p><strong>School user path</strong> (tenantId present in {@code TenantContext}):</p>
 * <ol>
 *   <li>Extract username from JWT {@code sub} claim.</li>
 *   <li>Load {@link User} from {@code users} table, scoped to {@code tenantId}.</li>
 *   <li>Validate token signature and username match.</li>
 *   <li>Set {@code UsernamePasswordAuthenticationToken} with the {@link User} as principal.</li>
 * </ol>
 *
 * <p><strong>Platform admin path</strong> (tenantId is {@code null} because
 * {@code TenantFilter} skips all {@code /api/platform/**} requests):</p>
 * <ol>
 *   <li>Extract username from JWT {@code sub} claim.</li>
 *   <li>Load {@link PlatformAdmin} from {@code platform_admins} table (no tenant scope).</li>
 *   <li>Validate token and set principal with {@code ROLE_PLATFORM_ADMIN} authority.</li>
 * </ol>
 *
 * <p>Public endpoints ({@code /api/auth/login}, {@code /api/auth/refresh}, etc.) are
 * excluded via {@link #shouldNotFilter} and never reach this logic.</p>
 */
@Component
@Order(2) // Run SECOND (after TenantFilter)
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String MDC_USER_ID = "userId";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // Extract JWT from Authorization header
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);

            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                log.debug("No JWT token found in request");
                filterChain.doFilter(request, response);
                return;
            }

            String jwt = authHeader.substring(BEARER_PREFIX.length());

            if (!jwtService.validateToken(jwt)) {
                log.warn("Invalid JWT token");
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            // Extract username from token
            String username = jwtService.extractUsername(jwt);

            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                log.debug("User already authenticated, skipping");
                filterChain.doFilter(request, response);
                return;
            }

            // Load user from database
            UUID tenantId = TenantContext.getCurrentTenantId();

            if (tenantId == null) {
                log.error("Tenant context not set - TenantFilter may have failed");
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            User user = userRepository.findByUsernameAndTenantId(username, tenantId)
                    .orElse(null);

            if (user == null) {
                log.warn("User not found: {} in tenant: {}", username, tenantId);
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            // Validate token matches user
            if (!jwtService.isTokenValid(jwt, user.getUsername())) {
                log.warn("Token-username mismatch");
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            user.getAuthorities()
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            MDC.put(MDC_USER_ID, user.getId().toString());

            log.info("User authenticated: {} (tenant: {})", username, tenantId);

        } catch (Exception e) {
            log.error("JWT authentication error: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_USER_ID);
        }
    }

    /**
     * Paths where JWT Bearer validation is intentionally skipped.
     *
     * /api/auth/refresh — user's access token may be expired; they're here to get a new one.
     *   The refresh JWT arrives via HttpOnly cookie, validated by RefreshTokenService.
     *
     * /api/auth/logout  — user may have an expired access token when logging out.
     *   Must still be able to revoke their refresh token and clear the cookie.
     *   Requiring a valid access token for logout would lock out users with expired tokens.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/login")     ||
                path.startsWith("/api/auth/register")  ||
                path.startsWith("/api/auth/refresh")   ||  // refresh token is in cookie, not Bearer
                path.startsWith("/api/auth/logout")    ||  // may have expired access token
                path.startsWith("/h2-console/")        ||
                path.startsWith("/actuator/health")    ||
                path.startsWith("/actuator/info")      ||
                path.startsWith("/swagger-ui/")        ||
                path.startsWith("/v3/api-docs/")       ||
                path.startsWith("/error");
    }
}
