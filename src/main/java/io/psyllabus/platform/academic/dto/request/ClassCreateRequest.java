package io.psyllabus.platform.academic.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request DTO for creating a new {@link io.psyllabus.platform.academic.entities.SchoolClass}.
 *
 * <p>{@code streamName} is validated against {@code SchoolProfile.streamNames} in
 * {@code ClassServiceImpl.createClass()} — prevents arbitrary free-text that breaks filtering.
 * If the school has not configured stream names, validation is skipped.</p>
 */
public record ClassCreateRequest(
    @NotBlank(message = "Class name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    @NotBlank(message = "Grade level is required")
    @Size(max = 50, message = "Grade level must not exceed 50 characters")
    String gradeLevel,

    /** Optional stream name — e.g. "Blue", "North". Validated against school profile config. */
    String streamName,

    @NotNull(message = "Academic year ID is required")
    UUID academicYearId,

    /** Optional: restricts this class to a specific branch. */
    UUID branchId,

    @Min(value = 1, message = "Max capacity must be at least 1")
    Integer maxCapacity
) {}
