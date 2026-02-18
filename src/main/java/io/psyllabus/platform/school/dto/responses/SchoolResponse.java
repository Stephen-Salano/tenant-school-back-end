package io.psyllabus.platform.school.dto.responses;

import io.psyllabus.platform.school.enums.SubscriptionPlan;

import java.util.UUID;

/**
 * GET /api/schools
 * { id, name, plan, isActive }
 */
public record SchoolResponse(
        UUID id,
        String name,
        String subdomainPrefix,
        String email,
        SubscriptionPlan plan,
        Boolean isActive
) {
}
