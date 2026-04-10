package io.psyllabus.platform.onboarding.service;

import io.psyllabus.platform.onboarding.dto.ImportErrorDetail;
import io.psyllabus.platform.onboarding.entity.ImportJob;
import io.psyllabus.platform.onboarding.enums.ImportJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing the lifecycle of bulk import jobs.
 */
public interface ImportJobService {

    /** Create a new job in QUEUED state. */
    ImportJob createJob(UUID tenantId, io.psyllabus.platform.onboarding.enums.ImportJobType type, int totalRows, List<Object> rawData);

    /** Fetch a single job; tenant-scoped. */
    ImportJob getJob(UUID jobId, UUID tenantId);

    /** Paginated history of jobs for a tenant. */
    Page<ImportJob> getTenantJobs(UUID tenantId, Pageable pageable);

    /** Update job progress from a listener batch. */
    ImportJob updateProgress(UUID jobId, UUID tenantId, int batchSuccesses, List<ImportErrorDetail> batchErrors);

    /** Transition job to terminal success/partial state. */
    void completeJob(UUID jobId, UUID tenantId);

    /** Transition job to FAILED state. */
    void failJob(UUID jobId, UUID tenantId, String reason);

    /** Delete a job record; tenant-scoped. */
    void deleteJob(UUID jobId, UUID tenantId);

    /** Cross-tenant monitoring for Platform Admins. */
    Page<ImportJob> getAllJobs(ImportJobStatus status, Pageable pageable);
}
