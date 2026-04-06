package io.psyllabus.platform.academic.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a new academic term.
 */
public record AcademicTermCreateRequest(
        @NotNull(message = "Academic year ID is required")
        UUID academicYearId,

        @NotBlank(message = "Term name is required")
        @Size(max = 50, message = "Name must not exceed 50 characters")
        String name,

        @Min(value = 1, message = "Term number must be at least 1")
        @Max(value = 3, message = "Term number must not exceed 3")
        int termNumber,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate
) {}
