package io.psyllabus.platform.academic.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight academic term response.
 */
public record AcademicTermResponse(
        UUID id,
        UUID academicYearId,
        String name,
        int termNumber,
        LocalDate startDate,
        LocalDate endDate,
        boolean isCurrent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
