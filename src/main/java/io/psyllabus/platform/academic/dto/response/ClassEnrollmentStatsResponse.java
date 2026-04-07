package io.psyllabus.platform.academic.dto.response;

import java.util.UUID;

/**
 * Enrollment statistics for a single class.
 * Returned by {@code GET /api/classes/{id}/enrollment-stats}.
 *
 * <p>{@code availableSlots} is 0 when {@code maxCapacity} is {@code null}
 * — the frontend should treat null capacity as "uncapped".</p>
 */
public record ClassEnrollmentStatsResponse(
    UUID classId,
    String className,
    int enrolledCount,
    Integer maxCapacity, // null = uncapped
    int availableSlots
) {}
