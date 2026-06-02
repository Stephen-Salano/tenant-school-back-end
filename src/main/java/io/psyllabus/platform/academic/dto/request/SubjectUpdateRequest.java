package io.psyllabus.platform.academic.dto.request;

import io.psyllabus.platform.academic.enums.AcademicStatus;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SubjectUpdateRequest(
        @Size(max = 100)
        String name,

        @Size(max = 20)
        String code,

        UUID subjectGroupId,
        UUID subjectCategoryId,
        Integer maxScore,
        Integer passMark,
        AcademicStatus status
) {
}
