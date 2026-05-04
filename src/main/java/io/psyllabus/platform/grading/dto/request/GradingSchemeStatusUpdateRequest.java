package io.psyllabus.platform.grading.dto.request;

import jakarta.validation.constraints.NotNull;

public record GradingSchemeStatusUpdateRequest(
        @NotNull(message = "Active flag is required")
        Boolean active
) {
}
