package io.psyllabus.platform.academic.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for {@link io.psyllabus.platform.academic.entities.Subject}.
 */
public record SubjectResponse(
    UUID id,
    String name,
    String code,
    UUID subjectGroupId,
    UUID subjectCategoryId,
    Integer maxScore,
    Integer passMark,
    boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
