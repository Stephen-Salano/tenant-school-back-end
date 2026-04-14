package io.psyllabus.platform.onboarding.listener;

import io.psyllabus.platform.common.annotation.SystemProcess;
import io.psyllabus.platform.common.config.RabbitMQConstants;
import io.psyllabus.platform.common.service.TenantExecutionService;
import io.psyllabus.platform.onboarding.entity.ImportJob;
import io.psyllabus.platform.onboarding.repository.ImportJobRepository;
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
public class StaffImportListener {
    private static final int BATCH_SIZE = 50;

    private final ImportJobRepository importJobRepository;
    private final TenantExecutionService tenantExecutionService;
    private final OnboardingBatchService onboardingBatchService;

    @RabbitListener(queues = RabbitMQConstants.STAFF_IMPORT_QUEUE)
    public void handle(OnboardingJobPayload payload) {
        UUID jobId = payload.jobId();
        UUID tenantId = payload.tenantId();

        tenantExecutionService.runInTenant(tenantId, () -> {
            ImportJob snapshot = importJobRepository.findByIdAndTenantId(jobId, tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Staff import job not found: " + jobId));
            processJob(jobId, tenantId, snapshot);
        });
    }

    private void processJob(UUID jobId, UUID tenantId, ImportJob started) {
        try {
            started = onboardingBatchService.markProcessing(jobId, tenantId);
            if (started.getStatus() == io.psyllabus.platform.onboarding.enums.ImportJobStatus.FAILED
                    || started.getStatus() == io.psyllabus.platform.onboarding.enums.ImportJobStatus.DONE
                    || started.getStatus() == io.psyllabus.platform.onboarding.enums.ImportJobStatus.COMPLETED_WITH_ERRORS) {
                log.info("Skipping staff import job in terminal state [jobId={}, status={}]", jobId, started.getStatus());
                return;
            }
            List<Map<String, String>> allRows = castRows(started.getRawData());
            int nextOffset = started.getProcessedRows();

            while (nextOffset < allRows.size()) {
                int end = Math.min(nextOffset + BATCH_SIZE, allRows.size());
                onboardingBatchService.processStaffBatch(jobId, tenantId, allRows.subList(nextOffset, end));
                nextOffset = end;
            }

            onboardingBatchService.finalizeJob(jobId, tenantId);
        } catch (Exception ex) {
            log.error("Staff import listener failed [jobId={}, tenantId={}]", jobId, tenantId, ex);
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
