package io.psyllabus.platform.onboarding.controller;

import io.psyllabus.platform.common.annotation.RequireFeature;
import io.psyllabus.platform.common.constants.FeatureConstants;
import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.onboarding.dto.response.ImportJobStatusResponse;
import io.psyllabus.platform.onboarding.service.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/onboarding")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Onboarding", description = "Bulk import for students and staff")
public class OnboardingController extends BaseController {

    private final OnboardingService onboardingService;

    @PostMapping(value = "/students/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequireFeature(FeatureConstants.STUDENT_MANAGEMENT)
    @Operation(summary = "Upload student + guardian CSV for bulk import")
    public ResponseEntity<ApiResponse<ImportJobStatusResponse>> uploadStudents(
            @RequestParam("file") MultipartFile file) {
        log.info("Student CSV upload [tenant={}, size={}]", getCurrentTenantId(), file.getSize());
        ImportJobStatusResponse response = onboardingService.uploadStudents(file, getCurrentTenantId());
        return created(response, "Student import job queued successfully");
    }

    @PostMapping(value = "/staff/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequireFeature(FeatureConstants.STAFF_MANAGEMENT)
    @Operation(summary = "Upload staff CSV for bulk import")
    public ResponseEntity<ApiResponse<ImportJobStatusResponse>> uploadStaff(
            @RequestParam("file") MultipartFile file) {
        log.info("Staff CSV upload [tenant={}, size={}]", getCurrentTenantId(), file.getSize());
        ImportJobStatusResponse response = onboardingService.uploadStaff(file, getCurrentTenantId());
        return created(response, "Staff import job queued successfully");
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Poll import job progress")
    public ResponseEntity<ApiResponse<ImportJobStatusResponse>> getJobStatus(
            @PathVariable UUID jobId) {
        return ok(onboardingService.getJobStatus(jobId, getCurrentTenantId()), "Job status retrieved");
    }

    @GetMapping("/jobs")
    @Operation(summary = "List all import jobs for this school (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<ImportJobStatusResponse>>> listJobs(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ok(onboardingService.listJobs(getCurrentTenantId(), pageable), "Import jobs retrieved");
    }

    @GetMapping("/jobs/{jobId}/errors")
    @Operation(summary = "Download error rows as CSV for a completed job")
    public ResponseEntity<byte[]> downloadErrors(@PathVariable UUID jobId) {
        log.info("Error CSV download [jobId={}, tenant={}]", jobId, getCurrentTenantId());
        byte[] csv = onboardingService.downloadErrorCsv(jobId, getCurrentTenantId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"import-errors-" + jobId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/jobs/{jobId}/completion-report")
    @Operation(summary = "Download full completion report as CSV for a finished job")
    public ResponseEntity<byte[]> downloadCompletionReport(@PathVariable UUID jobId) {
        log.info("Completion report download [jobId={}, tenant={}]", jobId, getCurrentTenantId());
        byte[] csv = onboardingService.downloadCompletionReportCsv(jobId, getCurrentTenantId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"import-completion-report-" + jobId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @PostMapping("/jobs/{jobId}/requeue")
    @Operation(summary = "Re-queue a FAILED import job for another attempt")
    public ResponseEntity<ApiResponse<ImportJobStatusResponse>> requeueJob(
            @PathVariable UUID jobId) {
        log.info("Job requeue requested [jobId={}, tenant={}]", jobId, getCurrentTenantId());
        return ok(onboardingService.requeueJob(jobId, getCurrentTenantId()), "Job re-queued for processing");
    }

    @DeleteMapping("/jobs/{jobId}")
    @Operation(summary = "Delete an import job (not allowed while PROCESSING)")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable UUID jobId) {
        log.info("Job delete requested [jobId={}, tenant={}]", jobId, getCurrentTenantId());
        onboardingService.deleteJob(jobId, getCurrentTenantId());
        return okMessage("Import job deleted successfully");
    }
}
