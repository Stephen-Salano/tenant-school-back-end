package io.psyllabus.platform.school.dto.responses;


import io.psyllabus.platform.subscription.enums.PlanType;

import java.util.UUID;

/**
 * General-purpose school view — used by authenticated admin endpoints (GET /api/schools/me).
 * Does NOT include admin credentials (those are only in SchoolOnboardResponse).
 */
public record SchoolResponse(
        UUID schoolId,
        String name,
        String subdomainPrefix,
        String email,
        PlanType plan,
        Boolean isActive
) {
}
