package io.psyllabus.platform.academic.dto.response;

import io.psyllabus.platform.academic.enums.AcademicStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for {@link io.psyllabus.platform.academic.entities.SubjectCategory}.
 */
public record SubjectCategoryResponse(
    UUID id,
    String name,
    String description,
    AcademicStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
