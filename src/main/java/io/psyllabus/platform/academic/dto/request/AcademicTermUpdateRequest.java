package io.psyllabus.platform.academic.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * PATCH request for updating an academic term.
 */
public record AcademicTermUpdateRequest(
        @Size(max = 50, message = "Name must not exceed 50 characters")
        String name,

        @Min(value = 1, message = "Term number must be at least 1")
        @Max(value = 3, message = "Term number must not exceed 3")
        Integer termNumber,

        LocalDate startDate,

        LocalDate endDate
) {}
