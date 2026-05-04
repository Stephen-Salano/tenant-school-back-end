package io.psyllabus.platform.grading.dto.request;

import io.psyllabus.platform.grading.enums.GradingSystem;
import jakarta.validation.Valid;

import java.util.List;

public record GradingSchemeUpdateRequest(
        String name,
        GradingSystem gradingSystem,
        Boolean isDefault,
        Boolean isActive,
        @Valid List<GradingLevelCreateRequest> levels
) {
}
