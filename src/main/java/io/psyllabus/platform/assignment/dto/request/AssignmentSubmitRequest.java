package io.psyllabus.platform.assignment.dto.request;

import jakarta.validation.Valid;

import java.util.List;

public record AssignmentSubmitRequest(
        String textContent,
        @Valid List<AttachmentMetadataRequest> attachments
) {
}
