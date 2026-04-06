package io.psyllabus.platform.academic.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight academic year response — used in list endpoints.
 * Does not include term details; see {@link AcademicYearDetailResponse} for richer single-fetch output.
 */
public record AcademicYearResponse(
        UUID id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        boolean isCurrent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
