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
 * JwtAuthFilter - Validates JWT and authenticates user.
 * 
 * Execution Order: SECOND (after TenantFilter)
 * 
 * Flow:
 * 1. Extract JWT from Authorization header
 * 2. Validate token (signature, expiration, issuer)
 * 3. Extract username from token
 * 4. Load User from database
 * 5. Create Authentication object
 * 6. Set SecurityContextHolder
 * 
 * Public Endpoints (Skip):
 * - /api/auth/login
 * - /api/auth/register
 * - /h2-console/** (dev only)
 * - /actuator/health
 * - /swagger-ui/**, /v3/api-docs/**
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
            
            log.debug("JWT token found, validating...");
            
            // Validate token
            if (!jwtService.validateToken(jwt)) {
                log.warn("Invalid JWT token");
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }
            
            // Extract username from token
            String username = jwtService.extractUsername(jwt);
            
            log.debug("JWT valid for user: {}", username);
            
            // Check if user is already authenticated
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
            
            User user = userRepository.findUsernameAndTenantId(username, tenantId)
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
            
            // Create authentication object
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(
                    user, // principal
                    null, // credentials (not needed - already authenticated)
                    user.getAuthorities() // roles/permissions
                );
            
            authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
            );
            
            // Set authentication in SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Set MDC for logging
            MDC.put(MDC_USER_ID, user.getId().toString());
            
            log.info("User authenticated: {} (tenant: {})", username, tenantId);
            
        } catch (Exception e) {
            log.error("JWT authentication error: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }

        // Continue filter chain
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Clear userId from MDC after request processing is complete
            MDC.remove(MDC_USER_ID);
        }
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Public endpoints - skip JWT validation
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/register") ||
               path.startsWith("/h2-console/") ||
               path.startsWith("/actuator/health") ||
               path.startsWith("/actuator/info") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/") ||
               path.startsWith("/error");
    }
}
