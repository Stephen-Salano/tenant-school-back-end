package io.psyllabus.platform.audit.dto.request;

import io.psyllabus.platform.audit.enums.AuditAction;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Criteria for exporting audit logs as CSV.
 *
 * <p>This request is persisted on the export job as JSONB so the background
 * worker can re-run the exact same query without depending on request state.</p>
 */
public record AuditExportRequest(
        AuditAction action,
        String entityType,
        UUID entityId,
        UUID userId,
        LocalDateTime fromDate,
        LocalDateTime toDate
) {}
