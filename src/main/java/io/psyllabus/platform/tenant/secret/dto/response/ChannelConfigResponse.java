package io.psyllabus.platform.tenant.secret.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChannelConfigResponse(
    UUID tenantId,
    boolean emailEnabled,
    boolean smsEnabled,
    String emailFromName,
    String emailReplyTo,
    boolean hasResendKey,
    boolean hasAtKey,
    LocalDateTime updatedAt
) {}
