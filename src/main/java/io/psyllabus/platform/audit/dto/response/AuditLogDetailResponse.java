package io.psyllabus.platform.audit.dto.response;

import io.psyllabus.platform.audit.enums.AuditAction;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Full audit log detail — used for single log retrieval and export.
 */
public record AuditLogDetailResponse(
        UUID id,
        UUID tenantId,
        UUID userId,
        String userName,
        AuditAction action,
        String entityType,
        UUID entityId,
        String details,
        String ipAddress,
        String userAgent,
        LocalDateTime createdAt
) {}