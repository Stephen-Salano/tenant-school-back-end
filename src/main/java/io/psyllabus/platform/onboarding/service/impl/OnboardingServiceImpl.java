package io.psyllabus.platform.onboarding.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.common.config.RabbitMQConstants;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.common.exception.CsvValidationException;
import io.psyllabus.platform.common.exception.MessagingException;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.onboarding.dto.response.ImportJobStatusResponse;
import io.psyllabus.platform.onboarding.dto.response.ValidationReport;
import io.psyllabus.platform.onboarding.entity.ImportJob;
import io.psyllabus.platform.onboarding.enums.ImportJobStatus;
import io.psyllabus.platform.onboarding.enums.ImportJobType;
import io.psyllabus.platform.onboarding.repository.ImportJobRepository;
import io.psyllabus.platform.onboarding.service.CsvValidationService;
import io.psyllabus.platform.onboarding.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OnboardingServiceImpl implements OnboardingService {

    private final ImportJobRepository importJobRepository;
    private final CsvValidationService csvValidationService;
    private final RabbitTemplate rabbitTemplate;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Value("${onboarding.max-file-size-bytes:5242880}")
    private long maxFileSizeBytes;

    private static final byte[][] BINARY_SIGNATURES = {
            new byte[]{0x25, 0x50, 0x44, 0x46}, // PDF
            new byte[]{0x50, 0x4B, 0x03, 0x04}, // ZIP/XLSX
            new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, // JPEG
            new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47} // PNG
    };

    @Override
    public ImportJobStatusResponse uploadStudents(MultipartFile file, UUID tenantId) {
        return processUpload(file, ImportJobType.STUDENT_BULK_IMPORT, tenantId, RabbitMQConstants.STUDENT_IMPORT_ROUTING_KEY);
    }

    @Override
    public ImportJobStatusResponse uploadStaff(MultipartFile file, UUID tenantId) {
        return processUpload(file, ImportJobType.STAFF_BULK_IMPORT, tenantId, RabbitMQConstants.STAFF_IMPORT_ROUTING_KEY);
    }

    @Override
    @Transactional(readOnly = true)
    public ImportJobStatusResponse getJobStatus(UUID jobId, UUID tenantId) {
        ImportJob job = findJobOrThrow(jobId, tenantId);
        return ImportJobStatusResponse.from(job);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ImportJobStatusResponse> listJobs(UUID tenantId, Pageable pageable) {
        Page<ImportJob> page = importJobRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        return PageResponse.from(page.map(ImportJobStatusResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadErrorCsv(UUID jobId, UUID tenantId) {
        ImportJob job = findJobOrThrow(jobId, tenantId);
        if (job.getErrorDetails() == null || job.getErrorDetails().isEmpty()) {
            throw new ResourceNotFoundException("No errors recorded for job " + jobId);
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(out, StandardCharsets.UTF_8),
                     CSVFormat.DEFAULT.builder().setHeader("row", "column", "issue", "raw_value").build())) {
            for (var err : job.getErrorDetails()) {
                printer.printRecord(err.row(), err.field(), err.issue(), err.rawValue());
            }
            printer.flush();
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Error building error CSV [jobId={}]", jobId, e);
            throw new MessagingException("Failed to generate error report", e);
        }
    }

    @Override
    public ImportJobStatusResponse requeueJob(UUID jobId, UUID tenantId) {
        ImportJob job = findJobOrThrow(jobId, tenantId);
        if (job.getStatus() != ImportJobStatus.FAILED) {
            throw new IllegalStateException("Only FAILED jobs can be re-queued. Current status: " + job.getStatus());
        }

        job.setStatus(ImportJobStatus.QUEUED);
        job.setProcessedRows(0);
        job.setSuccessRows(0);
        job.setErrorRows(0);
        job.getErrorDetails().clear();
        job.setCompletedAt(null);
        importJobRepository.save(job);

        String routingKey = job.getJobType() == ImportJobType.STUDENT_BULK_IMPORT ? 
                RabbitMQConstants.STUDENT_IMPORT_ROUTING_KEY : RabbitMQConstants.STAFF_IMPORT_ROUTING_KEY;
        
        rabbitTemplate.convertAndSend(RabbitMQConstants.ONBOARDING_EXCHANGE, routingKey, Map.of("jobId", job.getId().toString()));

        log.info("Job re-queued [jobId={}, tenantId={}]", jobId, tenantId);
        auditService.log(AuditAction.BULK_IMPORT, "IMPORT_JOB", jobId, "Job re-queued for retry");
        return ImportJobStatusResponse.from(job);
    }

    @Override
    public void deleteJob(UUID jobId, UUID tenantId) {
        ImportJob job = findJobOrThrow(jobId, tenantId);
        if (job.getStatus() == ImportJobStatus.PROCESSING) {
            throw new IllegalStateException("Cannot delete a job that is currently PROCESSING.");
        }
        importJobRepository.delete(job);
        log.info("Import job deleted [jobId={}, tenantId={}, status={}]", jobId, tenantId, job.getStatus());
        auditService.log(AuditAction.BULK_IMPORT, "IMPORT_JOB", jobId, "Job deleted by admin");
    }

    private ImportJobStatusResponse processUpload(MultipartFile file, ImportJobType jobType, UUID tenantId, String routingKey) {
        guardFile(file);
        List<Map<String, String>> rows = parseCsv(file);

        ValidationReport phase1 = csvValidationService.phaseOne(rows, jobType);
        if (!phase1.valid()) {
            log.warn("Phase 1 validation failed [tenant={}, errors={}]", tenantId, phase1.errorCount());
            throw new CsvValidationException(phase1);
        }

        ValidationReport phase2 = csvValidationService.phaseTwo(rows, jobType, tenantId);
        if (!phase2.valid()) {
            log.warn("Phase 2 validation failed [tenant={}, errors={}]", tenantId, phase2.errorCount());
            throw new CsvValidationException(phase2);
        }

        ImportJob job = ImportJob.create(tenantId, jobType, rows.size());
        job.setRawData(new ArrayList<>(rows));
        importJobRepository.save(job);

        rabbitTemplate.convertAndSend(RabbitMQConstants.ONBOARDING_EXCHANGE, routingKey, Map.of("jobId", job.getId().toString()));

        log.info("Import job queued [jobId={}, type={}, rows={}, tenantId={}]", job.getId(), jobType, rows.size(), tenantId);
        auditService.log(AuditAction.BULK_IMPORT, "IMPORT_JOB", job.getId(), "QUEUED " + rows.size() + " " + jobType.name() + " rows");

        return ImportJobStatusResponse.from(job);
    }

    private void guardFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload file is empty.");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("File exceeds limit. Size: " + (file.getSize() / 1024) + " KB");
        }
        try {
            byte[] header = file.getInputStream().readNBytes(4);
            for (byte[] sig : BINARY_SIGNATURES) {
                if (startsWith(header, sig)) {
                    throw new IllegalArgumentException("Invalid file type. Upload a plain CSV file.");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read uploaded file.", e);
        }
    }

    private List<Map<String, String>> parseCsv(MultipartFile file) {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {
            List<Map<String, String>> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                Map<String, String> row = record.toMap();
                boolean isInstruction = row.values().stream()
                        .anyMatch(v -> v != null && v.startsWith("[INSTRUCTIONS]"));
                if (!isInstruction) rows.add(row);
            }
            return rows;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage(), e);
        }
    }

    private ImportJob findJobOrThrow(UUID jobId, UUID tenantId) {
        return importJobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Import job not found: " + jobId));
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}
