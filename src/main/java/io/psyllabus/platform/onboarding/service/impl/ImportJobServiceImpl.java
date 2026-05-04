package io.psyllabus.platform.onboarding.service.impl;

import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.onboarding.dto.ImportErrorDetail;
import io.psyllabus.platform.onboarding.dto.ImportRowOutcome;
import io.psyllabus.platform.onboarding.entity.ImportJob;
import io.psyllabus.platform.onboarding.enums.ImportJobStatus;
import io.psyllabus.platform.onboarding.enums.ImportJobType;
import io.psyllabus.platform.onboarding.repository.ImportJobRepository;
import io.psyllabus.platform.onboarding.service.ImportJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ImportJobServiceImpl implements ImportJobService {

    private static final String IMPORT_JOB_NOT_FOUND = "Import job not found: ";
    private final ImportJobRepository importJobRepository;

    @Override
    public ImportJob createJob(UUID tenantId, ImportJobType type, int totalRows, List<Object> rawData) {
        ImportJob job = ImportJob.create(tenantId, type, totalRows);
        job.setRawData(rawData);
        log.info("Creating bulk import job: type={}, rows={}, tenant={}", type, totalRows, tenantId);
        return importJobRepository.save(job);
    }

    @Override
    @Transactional(readOnly = true)
    public ImportJob getJob(UUID jobId, UUID tenantId) {
        return findJobOrThrow(jobId, tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ImportJob> getTenantJobs(UUID tenantId, Pageable pageable) {
        return importJobRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    @Override
    public ImportJob updateProgress(UUID jobId, UUID tenantId, int batchSuccesses, List<ImportErrorDetail> batchErrors, List<ImportRowOutcome> batchOutcomes) {
        ImportJob job = findJobOrThrow(jobId, tenantId);
        job.markProcessing();
        job.recordBatchResult(batchSuccesses, batchErrors, batchOutcomes);
        log.debug("Job progress update: id={}, success={}, errors={}, total_processed={}", 
                jobId, job.getSuccessRows(), job.getErrorRows(), job.getProcessedRows());
        return importJobRepository.save(job);
    }

    @Override
    public void completeJob(UUID jobId, UUID tenantId) {
        ImportJob job = findJobOrThrow(jobId, tenantId);
        job.complete();
        importJobRepository.save(job);
        log.info("Import job completed: id={}, status={}", jobId, job.getStatus());
    }

    @Override
    public void failJob(UUID jobId, UUID tenantId, String reason) {
        ImportJob job = findJobOrThrow(jobId, tenantId);
        job.fail(reason);
        importJobRepository.save(job);
        log.warn("Import job failed: id={}, reason={}", jobId, reason);
    }

    @Override
    public void deleteJob(UUID jobId, UUID tenantId) {
        ImportJob job = findJobOrThrow(jobId, tenantId);
        importJobRepository.delete(job);
        log.info("Import job deleted: id={}, tenant={}", jobId, tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ImportJob> getAllJobs(ImportJobStatus status, Pageable pageable) {
        if (status != null) {
            return importJobRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable);
        }
        return importJobRepository.findAll(pageable);
    }

    private ImportJob findJobOrThrow(UUID jobId, UUID tenantId) {
        return importJobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(IMPORT_JOB_NOT_FOUND + jobId));
    }
}
