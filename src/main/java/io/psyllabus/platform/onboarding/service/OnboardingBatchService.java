package io.psyllabus.platform.onboarding.service;

import io.psyllabus.platform.onboarding.dto.ImportErrorDetail;
import io.psyllabus.platform.onboarding.dto.ImportRowOutcome;
import io.psyllabus.platform.onboarding.entity.ImportJob;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Batch-level onboarding persistence operations.
 *
 * <p>Each method runs in its own transaction boundary so one bad row or batch
 * does not roll back previously successful rows.</p>
 */
public interface OnboardingBatchService {

    ImportJob markProcessing(UUID jobId, UUID tenantId);

    String processOneStaffRow(Map<String, String> row, UUID tenantId);

    String processOneStudentRow(Map<String, String> row, UUID tenantId);

    BatchResult processStaffBatch(UUID jobId, UUID tenantId, List<Map<String, String>> rows);

    BatchResult processStudentBatch(UUID jobId, UUID tenantId, List<Map<String, String>> rows);

    ImportJob finalizeJob(UUID jobId, UUID tenantId);

    ImportJob failJob(UUID jobId, UUID tenantId, String reason);

    record BatchResult(
            int successCount,
            List<ImportErrorDetail> errors,
            List<ImportRowOutcome> outcomes
    ) {}
}
