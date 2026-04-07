package io.psyllabus.platform.academic.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request DTO for creating a new {@link io.psyllabus.platform.academic.entities.Subject}.
 */
public record SubjectCreateRequest(
    @NotBlank(message = "Subject name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    @Size(max = 20, message = "Code must not exceed 20 characters")
    String code,

    /** FK to {@link io.psyllabus.platform.academic.entities.SubjectGroup} */
    UUID subjectGroupId,

    /** FK to {@link io.psyllabus.platform.academic.entities.SubjectCategory} */
    UUID subjectCategoryId,

    /** Maximum achievable score in this subject. e.g. 100. */
    Integer maxScore,

    /** Minimum score to pass. e.g. 50. */
    Integer passMark
) {}
