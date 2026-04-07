package io.psyllabus.platform.academic.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for {@link io.psyllabus.platform.academic.entities.SubjectCategory}.
 */
public record SubjectCategoryResponse(
    UUID id,
    String name,
    String description,
    boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
