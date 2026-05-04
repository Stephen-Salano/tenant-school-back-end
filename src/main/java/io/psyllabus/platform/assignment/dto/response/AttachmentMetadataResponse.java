package io.psyllabus.platform.assignment.dto.response;

public record AttachmentMetadataResponse(
        String id,
        String url,
        String path,
        String mimeType,
        Long size,
        String originalFilename
) {
}
