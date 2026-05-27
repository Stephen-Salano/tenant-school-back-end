package io.psyllabus.platform.audit.controller;

import io.psyllabus.platform.audit.dto.request.AuditExportRequest;
import io.psyllabus.platform.audit.dto.request.AuditLogQueryRequest;
import io.psyllabus.platform.audit.dto.response.AuditExportJobResponse;
import io.psyllabus.platform.audit.dto.response.AuditExportJobStatusResponse;
import io.psyllabus.platform.audit.enums.AuditExportStatus;
import io.psyllabus.platform.audit.service.AuditExportService;
import io.psyllabus.platform.audit.dto.response.AuditLogResponse;
import io.psyllabus.platform.audit.service.AuditQueryService;
import io.psyllabus.platform.auth.enums.AccessScope;
import io.psyllabus.platform.auth.enums.UserType;
import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.platformadmin.entities.PlatformAdmin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/platform/audit")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Platform Audit", description = "Cross-tenant audit log query and export - PLATFORM_ADMIN only")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AuditController extends BaseController {

    private final AuditQueryService auditQueryService;
    private final AuditExportService auditExportService;

    @GetMapping
    @Operation(summary = "List audit logs (paginated, filterable)")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> listLogs(
            @Valid AuditLogQueryRequest query,
            Authentication authentication) {
        UUID adminId = extractPlatformAdminId(authentication);
        log.info("List platform audit logs request: adminId={}", adminId);
        
        var result = auditQueryService.listLogs(
                query, 
                adminId, 
                UserType.PLATFORM_ADMIN, 
                AccessScope.PLATFORM
        );
        return paged(result, "Audit logs retrieved");
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "List audit logs for a specific user")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> listByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Audit logs by user request: userId={}", userId);
        var result = auditQueryService.listByUser(userId, page, size);
        return paged(result, "Audit logs for user retrieved");
    }

    @PostMapping("/export")
    @Operation(summary = "Queue an audit-log export job")
    public ResponseEntity<ApiResponse<AuditExportJobResponse>> export(
            @Valid @RequestBody AuditExportRequest request,
            Authentication authentication) {
        UUID adminId = extractPlatformAdminId(authentication);
        log.info("Platform audit export request: adminId={}", adminId);

        var result = auditExportService.queueExport(request, adminId);
        return created(result, "Audit export queued");
    }

    @GetMapping("/export/{jobId}/status")
    @Operation(summary = "Get audit export job status")
    public ResponseEntity<ApiResponse<AuditExportJobStatusResponse>> getExportStatus(
            @PathVariable UUID jobId,
            Authentication authentication) {
        UUID adminId = extractPlatformAdminId(authentication);
        log.info("Platform audit export status request: adminId={}, jobId={}", adminId, jobId);

        AuditExportJobStatusResponse status = auditExportService.getJobStatus(jobId, adminId);
        return ok(withDownloadUrl(status), "Audit export status retrieved");
    }

    @GetMapping("/export/{jobId}/download")
    @Operation(summary = "Download a completed audit export CSV")
    public ResponseEntity<byte[]> downloadExport(
            @PathVariable UUID jobId,
            Authentication authentication) {
        UUID adminId = extractPlatformAdminId(authentication);
        log.info("Platform audit export download request: adminId={}, jobId={}", adminId, jobId);

        String csv = auditExportService.downloadExport(jobId, adminId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-export-" + jobId + ".csv\"")
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    private AuditExportJobStatusResponse withDownloadUrl(AuditExportJobStatusResponse status) {
        String downloadUrl = status.status() == AuditExportStatus.COMPLETED
                ? "/api/platform/audit/export/" + status.jobId() + "/download"
                : null;

        return new AuditExportJobStatusResponse(
                status.jobId(),
                status.status(),
                status.totalRecords(),
                status.requestedAt(),
                status.completedAt(),
                downloadUrl,
                status.failureReason()
        );
    }

    /**
     * Extract the platform admin's UUID from the {@code SecurityContext}.
     */
    private UUID extractPlatformAdminId(Authentication authentication) {
        if (authentication == null ||
                !(authentication.getPrincipal() instanceof PlatformAdmin admin)) {
            throw new IllegalStateException(
                    "Expected PlatformAdmin principal — check PlatformAdminAuthFilter");
        }
        return admin.getId();
    }
}
