package io.psyllabus.platform.audit.dto.response;

import io.psyllabus.platform.audit.entitiy.AuditExportJob;
import io.psyllabus.platform.audit.enums.AuditExportStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditExportJobStatusResponse(
        UUID jobId,
        AuditExportStatus status,
        Integer totalRecords,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        String downloadUrl,
        String failureReason
) {
    public static AuditExportJobStatusResponse from(AuditExportJob job, String downloadUrl) {
        return new AuditExportJobStatusResponse(
                job.getId(),
                job.getStatus(),
                job.getTotalRecords(),
                job.getCreatedAt(),
                job.getCompletedAt(),
                downloadUrl,
                job.getStatus() == AuditExportStatus.FAILED ? job.getFailureReason() : null
        );
    }
}
