package io.psyllabus.platform.audit.listener;

import io.psyllabus.platform.audit.dto.response.AuditLogResponse;
import io.psyllabus.platform.audit.entitiy.AuditExportJob;
import io.psyllabus.platform.audit.enums.AuditExportStatus;
import io.psyllabus.platform.audit.repository.AuditExportJobRepository;
import io.psyllabus.platform.audit.service.AuditQueryService;
import io.psyllabus.platform.common.annotation.SystemProcess;
import io.psyllabus.platform.common.config.RabbitMQConstants;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@SystemProcess
public class AuditExportListener {

    private static final Duration EXPORT_TTL = Duration.ofHours(24);
    private static final String REDIS_KEY_PREFIX = "audit-export:";

    private final AuditExportJobRepository auditExportJobRepository;
    private final AuditQueryService auditQueryService;
    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = RabbitMQConstants.AUDIT_EXPORT_QUEUE)
    public void handle(AuditExportJobPayload payload) {
        UUID jobId = payload.jobId();
        AuditExportJob job = auditExportJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Audit export job not found: " + jobId));

        if (job.getStatus() == AuditExportStatus.COMPLETED) {
            log.info("Skipping completed audit export job [jobId={}]", jobId);
            return;
        }
        if (job.getStatus() == AuditExportStatus.PROCESSING) {
            log.info("Skipping audit export job already in processing state [jobId={}]", jobId);
            return;
        }

        try {
            markProcessing(jobId);

            List<AuditLogResponse> logs = auditQueryService.listPlatformLogsForExport(job.getCriteria());
            String csv = buildCsv(logs);
            String redisKey = REDIS_KEY_PREFIX + jobId;

            stringRedisTemplate.opsForValue().set(redisKey, csv, EXPORT_TTL);
            markCompleted(jobId, redisKey, logs.size());

            log.info("Audit export job completed [jobId={}, totalRecords={}]", jobId, logs.size());
        } catch (RuntimeException ex) {
            markFailed(jobId, sanitizeFailureReason(ex));
            log.error("Audit export listener failed [jobId={}]", jobId, ex);
            throw ex;
        } catch (Exception ex) {
            markFailed(jobId, sanitizeFailureReason(ex));
            log.error("Audit export listener failed [jobId={}]", jobId, ex);
            throw new IllegalStateException("Audit export processing failed", ex);
        }
    }

    @Transactional
    protected void markProcessing(UUID jobId) {
        AuditExportJob job = auditExportJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Audit export job not found: " + jobId));
        job.markProcessing();
        auditExportJobRepository.save(job);
    }

    @Transactional
    protected void markCompleted(UUID jobId, String redisKey, int totalRecords) {
        AuditExportJob job = auditExportJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Audit export job not found: " + jobId));
        job.markCompleted(redisKey, totalRecords);
        auditExportJobRepository.save(job);
    }

    @Transactional
    protected void markFailed(UUID jobId, String failureReason) {
        AuditExportJob job = auditExportJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Audit export job not found: " + jobId));
        job.markFailed(failureReason);
        auditExportJobRepository.save(job);
    }

    private String buildCsv(List<AuditLogResponse> logs) throws IOException {
        try (StringWriter writer = new StringWriter();
             CSVPrinter printer = new CSVPrinter(
                     writer,
                     CSVFormat.DEFAULT.builder()
                             .setHeader(
                                     "id",
                                     "tenant_id",
                                     "user_id",
                                     "user_name",
                                     "action",
                                     "entity_type",
                                     "entity_id",
                                     "details",
                                     "ip_address",
                                     "created_at"
                             )
                             .build()
             )) {
            for (AuditLogResponse log : logs) {
                printer.printRecord(
                        sanitizeCsvCell(log.id()),
                        sanitizeCsvCell(log.tenantId()),
                        sanitizeCsvCell(log.userId()),
                        sanitizeCsvCell(log.userName()),
                        sanitizeCsvCell(log.action()),
                        sanitizeCsvCell(log.entityType()),
                        sanitizeCsvCell(log.entityId()),
                        sanitizeCsvCell(log.details()),
                        sanitizeCsvCell(log.ipAddress()),
                        sanitizeCsvCell(log.createdAt())
                );
            }
            printer.flush();
            return writer.toString();
        }
    }

    private String sanitizeCsvCell(Object value) {
        if (value == null) {
            return "";
        }

        String sanitized = value.toString()
                .replace("\r", " ")
                .replace("\n", " ")
                .replace("\t", " ")
                .replaceAll("\\p{Cntrl}", "")
                .trim();

        if (sanitized.startsWith("=")
                || sanitized.startsWith("+")
                || sanitized.startsWith("-")
                || sanitized.startsWith("@")) {
            return "'" + sanitized;
        }
        return sanitized;
    }

    private String sanitizeFailureReason(Throwable ex) {
        String message = ex.getMessage();
        if (!StringUtils.hasText(message)) {
            return "Audit export processing failed";
        }

        String sanitized = message
                .replace("\r", " ")
                .replace("\n", " ")
                .replace("\t", " ")
                .replaceAll("\\p{Cntrl}", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (sanitized.length() > 255) {
            return sanitized.substring(0, 255);
        }
        return sanitized;
    }
}
