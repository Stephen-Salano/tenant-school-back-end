package io.psyllabus.platform.audit.dto.response;

import io.psyllabus.platform.audit.entitiy.AuditExportJob;
import io.psyllabus.platform.audit.enums.AuditExportStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditExportJobResponse(
        UUID jobId,
        AuditExportStatus status,
        LocalDateTime requestedAt
) {
    public static AuditExportJobResponse from(AuditExportJob job) {
        return new AuditExportJobResponse(job.getId(), job.getStatus(), job.getCreatedAt());
    }
}
