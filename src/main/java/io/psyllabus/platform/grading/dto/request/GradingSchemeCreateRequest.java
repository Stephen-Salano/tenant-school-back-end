package io.psyllabus.platform.grading.dto.request;

import io.psyllabus.platform.grading.enums.GradingSystem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record GradingSchemeCreateRequest(
        @NotBlank(message = "Scheme name is required")
        String name,

        @NotNull(message = "Grading system is required")
        GradingSystem gradingSystem,

        Boolean isDefault,

        Boolean isActive,

        @Valid
        @NotEmpty(message = "At least one grading level is required")
        List<GradingLevelCreateRequest> levels
) {
}
