package io.psyllabus.platform.grading.dto.response;

import io.psyllabus.platform.grading.enums.GradingSystem;

import java.time.LocalDateTime;
import java.util.UUID;

public record GradingSchemeResponse(
        UUID id,
        String name,
        GradingSystem gradingSystem,
        boolean isDefault,
        boolean isActive,
        int levelCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
