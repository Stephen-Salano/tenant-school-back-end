package io.psyllabus.platform.onboarding.listener;

import io.psyllabus.platform.common.annotation.SystemProcess;
import io.psyllabus.platform.common.config.RabbitMQConstants;
import io.psyllabus.platform.common.service.TenantExecutionService;
import io.psyllabus.platform.onboarding.entity.ImportJob;
import io.psyllabus.platform.onboarding.enums.ImportJobStatus;

import io.psyllabus.platform.onboarding.service.OnboardingBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@SystemProcess
public class StudentImportListener {

    private static final int BATCH_SIZE = 50;

    private final TenantExecutionService tenantExecutionService;
    private final OnboardingBatchService onboardingBatchService;

    @RabbitListener(queues = RabbitMQConstants.STUDENT_IMPORT_QUEUE)
    public void handle(OnboardingJobPayload payload) {
        UUID jobId = payload.jobId();
        UUID tenantId = payload.tenantId();

        tenantExecutionService.runInTenant(tenantId, () ->
            // Removed redundant repository fetch
            processJob(jobId, tenantId)
        );
    }

    // Removed the ImportJob parameter
    private void processJob(UUID jobId, UUID tenantId) {
        try {
            // markProcessing handles the fetch
            ImportJob started = onboardingBatchService.markProcessing(jobId, tenantId);

            if (started.getStatus() == ImportJobStatus.FAILED
                    || started.getStatus() == ImportJobStatus.DONE
                    || started.getStatus() == ImportJobStatus.COMPLETED_WITH_ERRORS) {
                log.info("Skipping student import job in terminal state [jobId={}, status={}]", jobId, started.getStatus());
                return;
            }
            List<Map<String, String>> allRows = castRows(started.getRawData());
            int nextOffset = started.getProcessedRows();

            while (nextOffset < allRows.size()) {
                int end = Math.min(nextOffset + BATCH_SIZE, allRows.size());
                onboardingBatchService.processStudentBatch(jobId, tenantId, allRows.subList(nextOffset, end));
                nextOffset = end;
            }

            onboardingBatchService.finalizeJob(jobId, tenantId);
        } catch (Exception ex) {
            log.error("Student import listener failed [jobId={}, tenantId={}]", jobId, tenantId, ex);
            onboardingBatchService.failJob(jobId, tenantId, ex.getMessage() != null ? ex.getMessage() : "Unexpected listener failure");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> castRows(List<Object> rawData) {
        return rawData.stream()
                .map(row -> (Map<String, String>) row)
                .toList();
    }
}
