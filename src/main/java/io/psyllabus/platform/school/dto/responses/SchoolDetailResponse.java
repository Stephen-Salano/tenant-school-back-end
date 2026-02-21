package io.psyllabus.platform.school.dto.responses;

/**
 * GET /api/schools/{schoolId}
 * { schoolId, name, plan, address, branches[], subscriptionDetails }
 *
 * This is for richer data
 */
public record SchoolDetailResponse() {
}
