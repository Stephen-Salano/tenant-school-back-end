package io.psyllabus.platform.academic.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Rich class response — returned by {@code GET /api/classes/{id}}.
 *
 * <p>Includes computed enrollment fields derived from the students table.
 * {@code availableSlots} is 0 when {@code maxCapacity} is null (uncapped class).</p>
 *
 * <p>Also includes the current class teacher if assigned.</p>
 */
public record ClassDetailResponse(
    UUID id,
    String name,
    String gradeLevel,
    String streamName,
    UUID academicYearId,
    String academicYearName,
    UUID branchId,
    Integer maxCapacity,
    boolean isActive,
    int enrolledCount,
    int availableSlots,
    UUID classTeacherId,
    String classTeacherName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
