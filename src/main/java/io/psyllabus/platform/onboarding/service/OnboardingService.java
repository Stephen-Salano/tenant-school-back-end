package io.psyllabus.platform.onboarding.service;

import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.onboarding.dto.response.ImportJobStatusResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Manages the full lifecycle of CSV bulk-import jobs.
 */
public interface OnboardingService {

    /**
     * Validate a student CSV, create an import job, and publish to RabbitMQ.
     */
    ImportJobStatusResponse uploadStudents(MultipartFile file, UUID tenantId);

    /**
     * Validate a staff CSV, create an import job, and publish to RabbitMQ.
     */
    ImportJobStatusResponse uploadStaff(MultipartFile file, UUID tenantId);

    /** Poll progress for a single job. */
    ImportJobStatusResponse getJobStatus(UUID jobId, UUID tenantId);

    /** Paginated list of all import jobs for the tenant. */
    PageResponse<ImportJobStatusResponse> listJobs(UUID tenantId, Pageable pageable);

    /** Download error rows as a CSV file. */
    byte[] downloadErrorCsv(UUID jobId, UUID tenantId);

    /** Download a full completion report for a finished job. */
    byte[] downloadCompletionReportCsv(UUID jobId, UUID tenantId);

    /** Re-queue a FAILED job for another processing attempt. */
    ImportJobStatusResponse requeueJob(UUID jobId, UUID tenantId);

    /** Delete a job record. */
    void deleteJob(UUID jobId, UUID tenantId);
}
