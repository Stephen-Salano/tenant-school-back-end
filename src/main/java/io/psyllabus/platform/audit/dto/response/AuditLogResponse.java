package io.psyllabus.platform.audit.dto.response;

import io.psyllabus.platform.audit.enums.AuditAction;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit log list item — used in paginated list responses.
 */
public record AuditLogResponse(
        UUID id,
        UUID tenantId,
        UUID userId,
        String userName,
        AuditAction action,
        String entityType,
        UUID entityId,
        String details,
        String ipAddress,
        LocalDateTime createdAt
) {}