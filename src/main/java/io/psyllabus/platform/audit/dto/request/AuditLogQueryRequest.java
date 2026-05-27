package io.psyllabus.platform.audit.dto.request;

import io.psyllabus.platform.audit.enums.AuditAction;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Filter criteria for audit log queries.
 * All fields are optional — query returns all logs matching any non-null criteria.
 */
public record AuditLogQueryRequest(
        AuditAction action,
        String entityType,
        UUID entityId,
        UUID userId,
        LocalDateTime fromDate,
        LocalDateTime toDate,
        @NotNull(message = "Page number is required")
        Integer page,

        @NotNull(message = "Page size is required")
        Integer size
) {}