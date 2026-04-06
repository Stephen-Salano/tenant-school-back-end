package io.psyllabus.platform.academic.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or updating a {@link io.psyllabus.platform.academic.entities.SubjectGroup}.
 */
public record SubjectGroupRequest(
    @NotBlank(message = "Subject group name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description
) {}
