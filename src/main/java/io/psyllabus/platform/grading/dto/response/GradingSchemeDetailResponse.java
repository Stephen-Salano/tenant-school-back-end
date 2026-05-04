package io.psyllabus.platform.grading.dto.response;

import io.psyllabus.platform.grading.enums.GradingSystem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GradingSchemeDetailResponse(
        UUID id,
        String name,
        GradingSystem gradingSystem,
        boolean isDefault,
        boolean isActive,
        List<GradingLevelResponse> levels,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
