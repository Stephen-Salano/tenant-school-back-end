package io.psyllabus.platform.onboarding.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.common.config.RabbitMQConstants;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.common.exception.CsvValidationException;
import io.psyllabus.platform.common.exception.MessagingException;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.exception.ValidationException;
import io.psyllabus.platform.onboarding.dto.ImportErrorDetail;
import io.psyllabus.platform.onboarding.dto.ImportRowOutcome;
import io.psyllabus.platform.onboarding.dto.response.ImportJobStatusResponse;
import io.psyllabus.platform.onboarding.dto.response.ValidationReport;
import io.psyllabus.platform.onboarding.entity.ImportJob;
import io.psyllabus.platform.onboarding.enums.ImportJobStatus;
import io.psyllabus.platform.onboarding.enums.ImportJobType;
import io.psyllabus.platform.onboarding.listener.OnboardingJobPayload;
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

    private static final String IMPORT_JOB_LITERAL = "IMPORT_JOB";

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
                printer.printRecord(
                        err.row(),
                        sanitizeCsvCell(err.field()),
                        sanitizeCsvCell(err.issue()),
                        sanitizeCsvCell(err.rawValue())
                        );
            }
            printer.flush();
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Error building error CSV [jobId={}]", jobId, e);
            throw new MessagingException("Failed to generate error report", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadCompletionReportCsv(UUID jobId, UUID tenantId) {
        ImportJob job = findJobOrThrow(jobId, tenantId);
        validateJobReadyForReport(job);

        Map<Integer, List<ImportErrorDetail>> errorsByRow = mapErrorsByRow(job);
        Map<Integer, ImportRowOutcome> outcomesByRow = mapOutcomesByRow(job);
        
        List<Map<String, String>> rows = new ArrayList<>();
        Set<String> dynamicColumns = new LinkedHashSet<>();
        parseRawData(job, rows, dynamicColumns);

        List<String> headers = buildReportHeaders(dynamicColumns);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVPrinter printer = new CSVPrinter(
                     new OutputStreamWriter(out, StandardCharsets.UTF_8),
                     CSVFormat.DEFAULT.builder().setHeader(headers.toArray(String[]::new)).build()
             )) {

            for (int i = 0; i < rows.size(); i++) {
                int rowNumber = i + 1;
                Map<String, String> rowData = rows.get(i);
                List<ImportErrorDetail> rowErrors = errorsByRow.getOrDefault(rowNumber, List.of());
                ImportRowOutcome outcome = outcomesByRow.get(rowNumber);

                printer.printRecord(buildCsvRecordEntry(job, rowNumber, rowData, rowErrors, outcome, dynamicColumns));
            }
            printer.flush();
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Error building completion report CSV [jobId={}]", jobId, e);
            throw new MessagingException("Failed to generate completion report", e);
        }
    }

    private void validateJobReadyForReport(ImportJob job) {
        if (job.getCompletedAt() == null) {
            throw new ValidationException("Completion report is only available after the import job finishes.");
        }
        if (job.getRawData() == null || job.getRawData().isEmpty()) {
            throw new ResourceNotFoundException("Completion report data is no longer available for job " + job.getId());
        }
    }

    private Map<Integer, List<ImportErrorDetail>> mapErrorsByRow(ImportJob job) {
        Map<Integer, List<ImportErrorDetail>> errorsByRow = new HashMap<>();
        for (var err : job.getErrorDetails()) {
            if (err.row() > 0) {
                errorsByRow.computeIfAbsent(err.row(), ignored -> new ArrayList<>()).add(err);
            }
        }
        return errorsByRow;
    }

    private Map<Integer, ImportRowOutcome> mapOutcomesByRow(ImportJob job) {
        Map<Integer, ImportRowOutcome> outcomesByRow = new HashMap<>();
        if (job.getRowOutcomes() != null) {
            for (var outcome : job.getRowOutcomes()) {
                outcomesByRow.put(outcome.row(), outcome);
            }
        }
        return outcomesByRow;
    }

    private void parseRawData(ImportJob job, List<Map<String, String>> rows, Set<String> dynamicColumns) {
        for (Object rawRow : job.getRawData()) {
            Map<String, String> row = objectMapper.convertValue(rawRow, new TypeReference<>() {});
            rows.add(row);
            dynamicColumns.addAll(row.keySet());
        }
    }

    private List<String> buildReportHeaders(Set<String> dynamicColumns) {
        List<String> headers = new ArrayList<>(List.of(
                "row", "job_type", "status", "final_identifier",
                "failure_count", "failure_fields", "failure_issues", "failure_values"
        ));
        headers.addAll(dynamicColumns);
        return headers;
    }

    private List<String> buildCsvRecordEntry(
            ImportJob job, int rowNumber, Map<String, String> rowData,
            List<ImportErrorDetail> rowErrors, ImportRowOutcome outcome,
            Set<String> dynamicColumns
    ) {
        String rowStatus;
        String finalIdentifier = "";

        if (job.getStatus() == ImportJobStatus.FAILED && rowNumber > job.getProcessedRows()) {
            rowStatus = "NOT_PROCESSED";
        } else if (outcome != null) {
            rowStatus = outcome.status();
            finalIdentifier = outcome.finalIdentifier();
        } else {
            rowStatus = rowErrors.isEmpty() ? "SUCCESS" : "FAILED";
        }

        List<String> csvRecordEntry = new ArrayList<>();
        csvRecordEntry.add(String.valueOf(rowNumber));
        csvRecordEntry.add(job.getJobType().name());
        csvRecordEntry.add(rowStatus);
        csvRecordEntry.add(sanitizeCsvCell(finalIdentifier));
        csvRecordEntry.add(String.valueOf(rowErrors.size()));

        csvRecordEntry.add(sanitizeCsvCell(joinRowErrors(rowErrors, ImportErrorDetail::field)));
        csvRecordEntry.add(sanitizeCsvCell(joinRowErrors(rowErrors, ImportErrorDetail::issue)));
        csvRecordEntry.add(sanitizeCsvCell(joinRowErrors(rowErrors, ImportErrorDetail::rawValue)));

        for (String column : dynamicColumns) {
            csvRecordEntry.add(sanitizeCsvCell(rowData.getOrDefault(column, "")));
        }
        return csvRecordEntry;
    }

    @Override
    public ImportJobStatusResponse requeueJob(UUID jobId, UUID tenantId) {
        ImportJob job = findJobOrThrow(jobId, tenantId);
        if (job.getStatus() != ImportJobStatus.FAILED) {
            throw new ValidationException("Only FAILED jobs can be re-queued. Current status: " + job.getStatus());
        }

        if (job.getRawData() == null || job.getRawData().isEmpty()) {
            throw new ValidationException("Raw data has been cleared for this job. Re-upload the CSV to create a new import job.");
        }

        job.setStatus(ImportJobStatus.QUEUED);
        job.setProcessedRows(0);
        job.setSuccessRows(0);
        job.setErrorRows(0);
        job.setErrorDetails(new ArrayList<>());
        job.setRowOutcomes(new ArrayList<>());
        job.setCompletedAt(null);
        importJobRepository.save(job);

        String routingKey = job.getJobType() == ImportJobType.STUDENT_BULK_IMPORT ? 
                RabbitMQConstants.STUDENT_IMPORT_ROUTING_KEY : RabbitMQConstants.STAFF_IMPORT_ROUTING_KEY;
        
        rabbitTemplate.convertAndSend(RabbitMQConstants.ONBOARDING_EXCHANGE, routingKey, new OnboardingJobPayload(job.getId(), tenantId));

        log.info("Job re-queued [jobId={}, tenantId={}]", jobId, tenantId);
        auditService.log(AuditAction.BULK_IMPORT, IMPORT_JOB_LITERAL, jobId, "Job re-queued for retry");
        return ImportJobStatusResponse.from(job);
    }

    @Override
    public void deleteJob(UUID jobId, UUID tenantId) {
        ImportJob job = findJobOrThrow(jobId, tenantId);
        if (job.getStatus() == ImportJobStatus.PROCESSING) {
            throw new ValidationException("Cannot delete a job that is currently PROCESSING.");
        }
        importJobRepository.delete(job);
        log.info("Import job deleted [jobId={}, tenantId={}, status={}]", jobId, tenantId, job.getStatus());
        auditService.log(AuditAction.BULK_IMPORT, IMPORT_JOB_LITERAL, jobId, "Job deleted by admin");
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

        rabbitTemplate.convertAndSend(RabbitMQConstants.ONBOARDING_EXCHANGE, routingKey, new OnboardingJobPayload(job.getId(), job.getTenantId()));

        log.info("Import job queued [jobId={}, type={}, rows={}, tenantId={}]", job.getId(), jobType, rows.size(), tenantId);
        auditService.log(AuditAction.BULK_IMPORT, IMPORT_JOB_LITERAL, job.getId(), "QUEUED " + rows.size() + " " + jobType.name() + " rows");

        return ImportJobStatusResponse.from(job);
    }

    private void guardFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Upload file is empty.");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new ValidationException("File exceeds limit. Size: " + (file.getSize() / 1024) + " KB");
        }
        try {
            byte[] header = file.getInputStream().readNBytes(4);
            for (byte[] sig : BINARY_SIGNATURES) {
                if (startsWith(header, sig)) {
                    throw new ValidationException("Invalid file type. Upload a plain CSV file.");
                }
            }
        } catch (IOException e) {
            throw new ValidationException("Could not read uploaded file: " + e.getMessage());
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
            for (CSVRecord csvRecord : parser) {
                Map<String, String> row = csvRecord.toMap();
                boolean isInstruction = row.values().stream()
                        .anyMatch(v -> v != null && v.startsWith("[INSTRUCTIONS]"));
                if (!isInstruction) rows.add(row);
            }
            return rows;
        } catch (IOException e) {
            throw new ValidationException("Failed to parse CSV file: " + e.getMessage());
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

    private String joinRowErrors(
            List<io.psyllabus.platform.onboarding.dto.ImportErrorDetail> errors,
            java.util.function.Function<io.psyllabus.platform.onboarding.dto.ImportErrorDetail, String> extractor
    ) {
        return errors.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .distinct()
                .reduce((left, right) -> left + " | " + right)
                .orElse("");
    }

    private String sanitizeCsvCell(String value) {
        if (value == null) return "";
        String trimmed = value.strip();
        if (trimmed.startsWith("=") || trimmed.startsWith("+")
                || trimmed.startsWith("-") || trimmed.startsWith("@")) {
            return "'" + trimmed; // prefix with apostrophe — standard defense
        }
        return trimmed;
    }
}
