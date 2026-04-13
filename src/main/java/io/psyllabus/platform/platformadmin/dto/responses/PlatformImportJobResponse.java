package io.psyllabus.platform.platformadmin.dto.responses;

import io.psyllabus.platform.onboarding.enums.ImportJobStatus;
import io.psyllabus.platform.onboarding.enums.ImportJobType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Cross-tenant import-job view for platform oversight.
 */
public record PlatformImportJobResponse(
        UUID jobId,
        UUID tenantId,
        String schoolName,
        ImportJobType jobType,
        ImportJobStatus status,
        int totalRows,
        int processedRows,
        int successRows,
        int errorRows,
        int progressPercent,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}
