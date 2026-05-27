package io.psyllabus.platform.messaging.dto.request;

import io.psyllabus.platform.messaging.enums.MessageAudience;
import io.psyllabus.platform.messaging.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to post a new notice or announcement.
 */
public record MessageBoardRequest(
    @NotBlank @Size(max = 255) String title,
    @NotBlank String content,
    @NotNull MessageType type,
    @NotNull MessageAudience targetAudience,
    // TODO: Should take an image or the url storage not just string and can be null
    String attachmentUrl
) {}
