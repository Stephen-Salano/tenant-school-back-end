package io.psyllabus.platform.audit.service;

import io.psyllabus.platform.audit.dto.request.AuditExportRequest;
import io.psyllabus.platform.audit.dto.response.AuditExportJobResponse;
import io.psyllabus.platform.audit.dto.response.AuditExportJobStatusResponse;
import io.psyllabus.platform.audit.entitiy.AuditExportJob;
import io.psyllabus.platform.audit.event.AuditExportQueuedEvent;
import io.psyllabus.platform.audit.repository.AuditExportJobRepository;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuditExportServiceImpl implements AuditExportService {

    private static final String JOB_NOT_FOUND_MESSAGE = "Audit export job not found";

    private final AuditExportJobRepository auditExportJobRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public AuditExportJobResponse queueExport(AuditExportRequest request, UUID requestingUserId) {
        validateRequest(request);

        AuditExportJob job = AuditExportJob.queue(requestingUserId, request);
        AuditExportJob savedJob = auditExportJobRepository.save(job);
        eventPublisher.publishEvent(new AuditExportQueuedEvent(savedJob.getId()));

        log.info("Audit export job queued [jobId={}, requestingUserId={}]", savedJob.getId(), requestingUserId);
        return AuditExportJobResponse.from(savedJob);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditExportJobStatusResponse getJobStatus(UUID jobId, UUID requestingUserId) {
        AuditExportJob job = findOwnedJob(jobId, requestingUserId);
        return AuditExportJobStatusResponse.from(job, null);
    }

    @Override
    @Transactional(readOnly = true)
    public String downloadExport(UUID jobId, UUID requestingUserId) {
        AuditExportJob job = findOwnedJob(jobId, requestingUserId);
        if (!job.isCompleted()) {
            throw new ValidationException("Audit export is not ready for download");
        }
        if (!StringUtils.hasText(job.getRedisKey())) {
            throw new ResourceNotFoundException("Audit export file is no longer available. Request a new export.");
        }

        String csv = stringRedisTemplate.opsForValue().get(job.getRedisKey());
        if (!StringUtils.hasText(csv)) {
            throw new ResourceNotFoundException("Audit export file expired or is no longer available. Request a new export.");
        }
        return csv;
    }

    private AuditExportJob findOwnedJob(UUID jobId, UUID requestingUserId) {
        return auditExportJobRepository.findByIdAndRequestingUserId(jobId, requestingUserId)
                .orElseThrow(() -> new ResourceNotFoundException(JOB_NOT_FOUND_MESSAGE));
    }

    private void validateRequest(AuditExportRequest request) {
        if (request == null) {
            throw new ValidationException("Audit export request is required");
        }
        if (request.fromDate() != null && request.toDate() != null && request.fromDate().isAfter(request.toDate())) {
            throw new ValidationException("fromDate must be before or equal to toDate");
        }
    }
}
