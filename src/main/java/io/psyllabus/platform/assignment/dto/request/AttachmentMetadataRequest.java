package io.psyllabus.platform.assignment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AttachmentMetadataRequest(
        @NotBlank(message = "Attachment id is required")
        String id,

        @NotBlank(message = "Attachment URL is required")
        String url,

        @NotBlank(message = "Attachment path is required")
        String path,

        @NotBlank(message = "Attachment MIME type is required")
        String mimeType,

        @NotNull(message = "Attachment size is required")
        @Positive(message = "Attachment size must be positive")
        Long size,

        String originalFilename
) {
}
