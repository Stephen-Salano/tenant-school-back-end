package io.psyllabus.platform.academic.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new {@link io.psyllabus.platform.academic.entities.SubjectCategory}.
 */
public record SubjectCategoryCreateRequest(
    @NotBlank(message = "Category name is required")
    @Size(max = 50, message = "Name must not exceed 50 characters")
    String name,

    @Size(max = 255, message = "Description must not exceed 255 characters")
    String description
) {}
