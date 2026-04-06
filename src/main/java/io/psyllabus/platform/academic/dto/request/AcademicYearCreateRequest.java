package io.psyllabus.platform.academic.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request DTO for creating a new academic year.
 *
 * <p>Name must be unique within a tenant. Common formats: {@code "2026"} or
 * {@code "2025/2026"}. Service-layer validation ensures {@code startDate} is
 * strictly before {@code endDate}.</p>
 */
public record AcademicYearCreateRequest(

        @NotBlank(message = "Academic year name is required")
        @Size(max = 20, message = "Name must not exceed 20 characters")
        String name,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate
) {}
