package io.psyllabus.platform.onboarding.dto.response;

import io.psyllabus.platform.onboarding.entity.ImportJob;
import io.psyllabus.platform.onboarding.enums.ImportJobStatus;
import io.psyllabus.platform.onboarding.enums.ImportJobType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Returned from all job endpoints — upload, poll, list.
 * {@code progressPercent} is computed here so the entity stays clean.
 */
public record ImportJobStatusResponse(
    UUID jobId,
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
    public static ImportJobStatusResponse from(ImportJob job) {
        int pct = job.getTotalRows() == 0 ? 0 : (int) ((job.getProcessedRows() * 100.0) / job.getTotalRows());
        return new ImportJobStatusResponse(
            job.getId(),
            job.getJobType(),
            job.getStatus(),
            job.getTotalRows(),
            job.getProcessedRows(),
            job.getSuccessRows(),
            job.getErrorRows(),
            pct,
            job.getCreatedAt(),
            job.getCompletedAt()
        );
    }
}
