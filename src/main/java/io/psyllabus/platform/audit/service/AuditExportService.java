package io.psyllabus.platform.audit.service;

import io.psyllabus.platform.audit.dto.request.AuditExportRequest;
import io.psyllabus.platform.audit.dto.response.AuditExportJobResponse;
import io.psyllabus.platform.audit.dto.response.AuditExportJobStatusResponse;

import java.util.UUID;

public interface AuditExportService {

    AuditExportJobResponse queueExport(AuditExportRequest request, UUID requestingUserId);

    AuditExportJobStatusResponse getJobStatus(UUID jobId, UUID requestingUserId);

    String downloadExport(UUID jobId, UUID requestingUserId);
}
