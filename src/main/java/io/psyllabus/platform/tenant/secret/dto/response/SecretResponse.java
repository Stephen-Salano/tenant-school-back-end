package io.psyllabus.platform.tenant.secret.dto.response;

import io.psyllabus.platform.tenant.secret.enums.SecretType;
import java.time.LocalDateTime;
import java.util.UUID;

public record SecretResponse(
    UUID id,
    SecretType secretType,
    String maskedValue,
    String description,
    boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
