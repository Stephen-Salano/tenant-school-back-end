package io.psyllabus.platform.common.service;

import io.psyllabus.platform.common.exception.TenantNotFoundException;
import io.psyllabus.platform.school.entities.School;
import io.psyllabus.platform.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantResolutionService {

    private final SchoolRepository schoolRepository;

    /**
     * Resolve tenant ID by subdomain prefix.
     * Caches the result to avoid DB lookups on every request.
     *
     * @param subdomainPrefix the subdomain prefix (e.g., "stmary")
     * @return the tenant ID
     * @throws TenantNotFoundException if tenant not found or inactive
     */
//    @Cacheable(value = "tenant-subdomain", key = "#subdomainPrefix", unless = "#result == null")
    public UUID resolveBySubdomain(String subdomainPrefix) {
        log.debug("Resolving tenant by subdomain: {}", subdomainPrefix);
        School school = schoolRepository.findBySubdomainPrefix(subdomainPrefix)
                .orElseThrow(() -> new TenantNotFoundException(
                        "School not found for subdomain: " + subdomainPrefix
                ));

        if (!school.getIsActive()) {
            throw new TenantNotFoundException("School is inactive: " + subdomainPrefix);
        }

        return school.getId();
    }

    /**
     * Resolve tenant ID by custom domain.
     * Caches the result to avoid DB lookups on every request.
     *
     * @param customDomain the custom domain (e.g., "portal.university.edu")
     * @return the tenant ID
     * @throws TenantNotFoundException if tenant not found or inactive
     */
//    @Cacheable(value = "tenant-custom-domain", key = "#customDomain", unless = "#result == null")
    public UUID resolveByCustomDomain(String customDomain) {
        log.debug("Resolving tenant by custom domain: {}", customDomain);
        School school = schoolRepository.findByCustomDomain(customDomain)
                .orElseThrow(() -> new TenantNotFoundException(
                        "School not found for custom domain: " + customDomain
                ));

        if (!school.getIsActive()) {
            throw new TenantNotFoundException("School is inactive: " + customDomain);
        }

        return school.getId();
    }

    /**
     * Resolve tenant ID by ID (for dev/test headers).
     * Caches the result.
     *
     * @param tenantId the tenant ID
     * @return the tenant ID
     * @throws TenantNotFoundException if tenant not found or inactive
     */
//    @Cacheable(value = "tenant-id", key = "#tenantId", unless = "#result == null")
    public UUID resolveById(UUID tenantId) {
        log.debug("Resolving tenant by ID: {}", tenantId);
        School school = schoolRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(
                        "School not found for ID: " + tenantId
                ));

        if (!school.getIsActive()) {
            throw new TenantNotFoundException("School is inactive: " + tenantId);
        }

        return school.getId();
    }
}
