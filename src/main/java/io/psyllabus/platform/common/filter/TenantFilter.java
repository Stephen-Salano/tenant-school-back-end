package io.psyllabus.platform.common.filter;

import io.psyllabus.platform.common.exception.SubdomainResolutionException;
import io.psyllabus.platform.common.exception.TenantNotFoundException;
import io.psyllabus.platform.common.service.TenantResolutionService;
import io.psyllabus.platform.common.util.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

/**
 * TenantFilter - Resolves tenant from subdomain BEFORE authentication.
 *
 * Execution Order: FIRST (before JwtAuthFilter)
 *
 * Flow:
 * 1. Check for X-Tenant-ID header (Dev/Test only)
 * 2. Extract Host header (e.g., "student.stmary.ourcompany.com")
 * 3. Determine if Subdomain or Custom Domain
 * 4. Query schools table (via Cached Service)
 * 5. Set TenantContext.setCurrentTenant(school.schoolId)
 * 6. Continue filter chain
 * 7. Clear context in finally block
 *
 * Security:
 * - Tenant MUST come from subdomain/domain, NEVER from user input in Prod
 * - Validates school is active before setting context
 * - Throws exception if subdomain invalid or school not found
 */

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // Run after RequestIdFilter
@RequiredArgsConstructor
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

    private final TenantResolutionService tenantResolutionService;
    private final Environment environment;

    @Value("${tenant.domain.base}")
    private String BASE_DOMAIN;
    
    @Value("${tenant.resolution.header-name:X-Tenant-ID}")
    private String TENANT_HEADER_NAME;

    private static final String SUBDOMAIN_SEPARATOR = ".";
    private static final String MDC_TENANT_ID = "tenantId";


    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try{
            // 1. Dev/Test Override: Check for X-Tenant-ID header
            if (isDevOrTest()) {
                String headerTenantId = request.getHeader(TENANT_HEADER_NAME);
                if (headerTenantId != null && !headerTenantId.isBlank()) {
                    log.debug("Dev mode: Using {} header: {}", TENANT_HEADER_NAME, headerTenantId);
                    resolveTenantByHeader(headerTenantId);
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            String host = request.getHeader("Host");

            if (host == null || host.isBlank()){
                log.warn("Missing Host header in request");
                throw new SubdomainResolutionException("Missing Host Header");
            }

            // Remove port if present (e.g., localhost:8080)
            String cleanHost = host.split(":")[0];
            log.debug("Processing request for host: {}", cleanHost);

            UUID tenantId;

            // 2. Determine Resolution Strategy
            if (cleanHost.endsWith(BASE_DOMAIN)) {
                // Scenario 1: Subdomain (e.g., student.stmary.ourcompany.com)
                String subdomainPrefix = extractSubDomainPrefix(cleanHost);
                log.debug("Extracted subdomain prefix: {}", subdomainPrefix);
                
                tenantId = tenantResolutionService.resolveBySubdomain(subdomainPrefix);
            } else {
                // Scenario 2: Custom Domain (e.g., portal.university.edu)
                log.debug("Treating as custom domain: {}", cleanHost);
                
                tenantId = tenantResolutionService.resolveByCustomDomain(cleanHost);
            }

            // 3. Set Context
            TenantContext.setCurrentTenant(tenantId);
            MDC.put(MDC_TENANT_ID, tenantId.toString());
            log.info("Tenant Context set: tenant_id={}", tenantId);

            filterChain.doFilter(request, response);
            
        } catch (TenantNotFoundException | SubdomainResolutionException e) {
            log.error("Tenant resolution failed: {}", e.getMessage());
            // We don't want to expose internal details, but 404 is appropriate here
            response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } finally {
            TenantContext.clear();
            MDC.remove(MDC_TENANT_ID);
            log.debug("Tenant context cleared");
        }
    }

    private void resolveTenantByHeader(String tenantIdentifier) {
        UUID tenantId;
        try {
            UUID id = UUID.fromString(tenantIdentifier);
            tenantId = tenantResolutionService.resolveById(id);
        } catch (IllegalArgumentException e) {
            // Not a UUID, try subdomain
            tenantId = tenantResolutionService.resolveBySubdomain(tenantIdentifier);
        }

        TenantContext.setCurrentTenant(tenantId);
        MDC.put(MDC_TENANT_ID, tenantId.toString());
        log.info("Tenant Context set via Header: tenant_id={}", tenantId);
    }

    /**
     * Extract subdomain prefix from hostname.
     *
     * Examples:
     * - "student.stmary.ourcompany.com" → "stmary"
     * - "stmary.ourcompany.com" → "stmary"
     *
     * @param host Full hostname from request (without port)
     * @return Subdomain prefix (school identifier)
     */
    private String extractSubDomainPrefix(String host) {
        // Remove base domain
        String withoutSuffix = host.substring(0, host.length() - BASE_DOMAIN.length());
        
        // Remove trailing dot if present (it should be)
        if (withoutSuffix.endsWith(SUBDOMAIN_SEPARATOR)) {
            withoutSuffix = withoutSuffix.substring(0, withoutSuffix.length() - 1);
        }

        // Split by dots to handle multi-level subdomains
        // e.g., "student.stmary" -> ["student", "stmary"]
        String[] parts = withoutSuffix.split("\\" + SUBDOMAIN_SEPARATOR);

        if (parts.length == 0 || (parts.length == 1 && parts[0].isBlank())) {
             throw new SubdomainResolutionException("Invalid subdomain format");
        }

        // We assume the LAST part is the school identifier
        // e.g., "student.stmary" -> "stmary"
        // e.g., "stmary" -> "stmary"
        return parts[parts.length - 1];
    }

    private boolean isDevOrTest() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("dev") || p.equalsIgnoreCase("test"));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") ||
                path.startsWith("/actuator/info") ||
                path.startsWith("/error") ||
                path.startsWith("/h2-console") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.equals("/api/schools/onboard") || //TODO: remove this later on and add platform admin issue #35
                path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/register");
    }
}
