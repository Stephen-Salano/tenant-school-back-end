package io.psyllabus.platform.academic.dto.request;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * PATCH request for updating an academic year.
 *
 * <p>All fields are nullable — only non-null values are applied (PATCH semantics).
 * If both {@code startDate} and {@code endDate} are provided, the service validates
 * that the effective start date remains strictly before the effective end date.</p>
 */
public record AcademicYearUpdateRequest(

        @Size(max = 20, message = "Name must not exceed 20 characters")
        String name,

        LocalDate startDate,

        LocalDate endDate
) {}
