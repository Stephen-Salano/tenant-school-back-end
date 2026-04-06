package io.psyllabus.platform.academic.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Rich academic year response — returned by {@code GET /api/academic/years/{id}}.
 *
 * <p>{@code termCount} reflects the number of terms configured for this year.
 * It is populated once SM3 Task 3.3 (Academic Term Service) is complete.
 * Until then the service sets it to {@code 0}.</p>
 */
public record AcademicYearDetailResponse(
        UUID id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        boolean isCurrent,
        int termCount,          // wired in Task 3.3 — returns 0 until then
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
