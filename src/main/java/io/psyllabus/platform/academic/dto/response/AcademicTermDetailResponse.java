package io.psyllabus.platform.academic.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Rich academic term response.
 */
public record AcademicTermDetailResponse(
        UUID id,
        UUID academicYearId,
        String academicYearName,
        String name,
        int termNumber,
        LocalDate startDate,
        LocalDate endDate,
        boolean isCurrent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
