package io.psyllabus.platform.assignment.dto.request;

import io.psyllabus.platform.assignment.enums.AssignmentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AssignmentCreateRequest(
        @NotNull(message = "Class is required")
        UUID classId,

        @NotNull(message = "Subject is required")
        UUID subjectId,

        @NotBlank(message = "Title is required")
        String title,

        String description,

        String instructions,

        @NotNull(message = "Assignment type is required")
        AssignmentType assignmentType,

        @NotNull(message = "Max score is required")
        @Min(value = 1, message = "Max score must be at least 1")
        Integer maxScore,

        @NotNull(message = "Due date is required")
        LocalDateTime dueDate,

        @Valid List<AttachmentMetadataRequest> attachments
) {
}
