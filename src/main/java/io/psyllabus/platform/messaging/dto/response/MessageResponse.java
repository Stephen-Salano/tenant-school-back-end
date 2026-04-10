package io.psyllabus.platform.messaging.dto.response;

import io.psyllabus.platform.messaging.enums.MessageAudience;
import io.psyllabus.platform.messaging.enums.MessageType;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
    UUID id,
    MessageType type,
    MessageAudience targetAudience,
    String title,
    String content,
    String attachmentUrl,
    String recipientIdentifier,
    LocalDateTime createdAt
) {}
