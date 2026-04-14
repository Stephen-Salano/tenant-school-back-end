package io.psyllabus.platform.onboarding.listener;

import io.psyllabus.platform.auth.enums.UserType;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.messaging.dto.EmailMessage;
import io.psyllabus.platform.messaging.enums.EmailTemplateType;
import io.psyllabus.platform.messaging.service.EmailService;
import io.psyllabus.platform.onboarding.entity.ImportJob;
import io.psyllabus.platform.onboarding.event.ImportJobFinalizedEvent;
import io.psyllabus.platform.school.entities.School;
import io.psyllabus.platform.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.UUID;

/**
 * Handles post-onboarding notifications.
 *
 * <p>Uses {@code AFTER_COMMIT} to ensure we don't send emails if the
 * {@code REQUIRES_NEW} transaction in OnboardingBatchService rolls back.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ImportJobNotificationListener {

    private final EmailService emailService;
    private final SchoolRepository schoolRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJobFinalized(ImportJobFinalizedEvent event) {
        ImportJob job = event.getJob();
        log.info("Handling finalized job notification [jobId={}, status={}]", job.getId(), job.getStatus());

        try {
            School school = schoolRepository.findByIdCrossTenant(job.getTenantId())
                    .orElseThrow(() -> new ResourceNotFoundException("School not found: " + job.getTenantId()));

            if (school.getEmail() == null || school.getEmail().isBlank()) {
                log.warn("School admin email not configured for tenant {}, skipping notification", job.getTenantId());
                return;
            }

            EmailTemplateType templateType = job.getStatus() == io.psyllabus.platform.onboarding.enums.ImportJobStatus.FAILED
                    ? EmailTemplateType.IMPORT_FAILED
                    : EmailTemplateType.IMPORT_COMPLETE_SUMMARY;

            EmailMessage message = new EmailMessage(
                    UUID.randomUUID().toString(),
                    job.getTenantId(),
                    school.getEmail(),
                    school.getName() + " Admin",
                    UserType.ADMIN,
                    templateType,
                    Map.of(
                            "successCount", String.valueOf(job.getSuccessRows()),
                            "failureCount", String.valueOf(job.getErrorRows()),
                            "totalRows", String.valueOf(job.getTotalRows()),
                            "jobType", job.getJobType().name(),
                            "jobId", job.getId().toString(),
                            "toName", school.getName() + " Admin"
                    )
            );

            emailService.send(message);
            log.info("Admin notification triggered for import job {} [status={}]", job.getId(), job.getStatus());
        } catch (Exception ex) {
            log.error("Failed to send admin notification for job {}", job.getId(), ex);
        }
    }
}
