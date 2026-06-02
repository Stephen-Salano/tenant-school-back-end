package io.psyllabus.platform.assignment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record BulkGradeRequest(
        @NotEmpty(message = "At least one grade entry is required")
        List<GradeEntry> grades
) {
    public record GradeEntry(
            @NotNull(message = "Submission ID is required")
            UUID submissionId,

            @NotNull(message = "Score is required")
            @Min(value = 0, message = "Score cannot be negative")
            int score,

            String feedback
    ) {}
}
