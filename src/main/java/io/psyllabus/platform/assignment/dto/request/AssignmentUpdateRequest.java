package io.psyllabus.platform.assignment.dto.request;

import io.psyllabus.platform.assignment.enums.AssignmentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;
import java.util.List;

public record AssignmentUpdateRequest(
        String title,
        String description,
        String instructions,
        AssignmentType assignmentType,
        @Min(value = 1, message = "Max score must be at least 1") Integer maxScore,
        LocalDateTime dueDate,
        @Valid List<AttachmentMetadataRequest> attachments
) {
}
