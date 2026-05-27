package io.psyllabus.platform.platformadmin.common.filter;

import io.psyllabus.platform.auth.enums.UserType;
import io.psyllabus.platform.auth.services.JwtService;
import io.psyllabus.platform.platformadmin.entities.PlatformAdmin;
import io.psyllabus.platform.platformadmin.repositories.PlatformAdminRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates platform-level administrator requests on {@code /api/platform/**}.
 *
 * <p>This filter exists as a <strong>separate concern</strong> from {@link io.psyllabus.platform.common.filter.JwtAuthFilter}
 * precisely because platform admins and school users are different identity domains:</p>
 * <ul>
 *   <li>Different DB table ({@code platform_admins} vs {@code users})</li>
 *   <li>Different token shape (no {@code tenant_id} claim)</li>
 *   <li>Different Spring Security authority ({@code ROLE_PLATFORM_ADMIN})</li>
 * </ul>
 *
 * <p>Merging this logic into {@link io.psyllabus.platform.common.filter.JwtAuthFilter} would require reasoning about
 * null {@code tenantId} as an identity signal — which is fragile (null tenantId can
 * occur due to filter errors, not just platform admin requests). Keeping them separate
 * means each filter has one clear job and one clear failure mode.</p>
 *
 * <p><strong>Filter chain order:</strong></p>
 * <pre>
 * Order 1 — PlatformKeyFilter      (API key guard for /api/platform/**)
 * Order 2 — PlatformAdminAuthFilter (JWT auth for /api/platform/**)   ← this class
 * Order 3 — TenantFilter            (tenant resolution, skips /api/platform/**)
 * Order 4 — JwtAuthFilter           (JWT auth for school paths, skips /api/platform/**)
 * </pre>
 *
 * <p><strong>Two-gate validation</strong> on every platform admin request:</p>
 * <ol>
 *   <li>JWT signature + expiry ({@code jwtService.validateToken})</li>
 *   <li>{@code user_type} claim must equal {@code PLATFORM_ADMIN} — rejects any
 *       school-user token that somehow reaches a platform endpoint</li>
 *   <li>Username from {@code sub} must match a record in {@code platform_admins}</li>
 * </ol>
 *
 * <p>K-DPA: no school or student data is accessible from this filter. All log
 * statements deliberately exclude PII (no email, no phone).</p>
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class PlatformAdminAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final PlatformAdminRepository platformAdminRepository;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String MDC_USER_ID = "userId";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try{
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);

            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)){
                log.warn("Missing Bearer token on platform endpoint: {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            String jwt = authHeader.substring(BEARER_PREFIX.length());

            // Gate 1: Signature, expiry, issuer, audience
            if (!jwtService.validateToken(jwt)){
                log.warn("Invalid JWT on platform endpoint: {}", request.getRequestURI());
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            // Gate 2 - user_type claim must be PLATFORM_ADMIN
            // Rejects any school user-token (ADMIN, STAFF, etc) that reaches this path
            UserType userType = jwtService.extractUserType(jwt);
            if (userType != UserType.PLATFORM_ADMIN){
                log.warn("Non-platform token on platform endpoint. user_type={}, path={}", userType, request.getRequestURI());
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            // Gate 3: Load principal from platform_admins (no tenant scope)
            String username = jwtService.extractUsername(jwt);
            PlatformAdmin admin = platformAdminRepository.findByUsername(username).orElse(null);

            if (admin == null){
                log.warn("Platform admin not found for username in JWT sub claim");
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            if (!jwtService.isTokenValid(jwt, admin.getUsername())){
                log.warn("Token sub claim does not match loaded platform admin");
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    admin, null, admin.getAuthorities()
            );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            MDC.put(MDC_USER_ID, admin.getId().toString());
            log.info("Platform admin authenticated: id={}", admin.getId());
        } catch (Exception e){
            log.error("Platform admin auth error: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }
        try{
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_USER_ID);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request){
        String path = request.getRequestURI();
        return !path.startsWith("/api/platform/") || path.startsWith("/api/platform/auth/");
    }
}
