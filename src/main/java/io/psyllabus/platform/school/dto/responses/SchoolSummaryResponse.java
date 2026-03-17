package io.psyllabus.platform.school.dto.responses;

import io.psyllabus.platform.subscription.enums.PlanType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight school summary — used by cross-tenant list and detail endpoints.
 *
 * <p>Intentionally lighter than {@link SchoolProvisionResponse}: no temp password,
 * no provisioning notes. Safe to return on repeated fetches.</p>
 *
 * @param schoolId        UUID of the school
 * @param schoolName      name of the school
 * @param contactEmail    school's primary contact email
 * @param planType        current subscription plan
 * @param isActive        whether the school is currently active
 * @param subdomainPrefix subdomain slug, or {@code null} if using a custom domain
 * @param customDomain    custom domain, or {@code null} if using subdomain
 * @param provisionedAt   when the school was provisioned
 * @param provisionedBy   UUID of the platform admin who provisioned this school
 */
public record SchoolSummaryResponse(
        UUID schoolId,
        String schoolName,
        String contactEmail,
        PlanType planType,
        boolean isActive,
        String subdomainPrefix,
        String customDomain,
        LocalDateTime provisionedAt,
        UUID provisionedBy
) {
}
