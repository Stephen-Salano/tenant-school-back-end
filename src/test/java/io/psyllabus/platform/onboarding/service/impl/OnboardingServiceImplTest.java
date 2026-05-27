package io.psyllabus.platform.onboarding.service.impl;

import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.common.exception.ValidationException;
import io.psyllabus.platform.onboarding.entity.ImportJob;
import io.psyllabus.platform.onboarding.enums.ImportJobStatus;
import io.psyllabus.platform.onboarding.enums.ImportJobType;
import io.psyllabus.platform.onboarding.repository.ImportJobRepository;
import io.psyllabus.platform.onboarding.service.CsvValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OnboardingServiceImplTest {

    @Test
    void downloadCompletionReportCsv_throwsValidationExceptionWhenJobNotComplete() {
        ImportJobRepository importJobRepository = mock(ImportJobRepository.class);
        UUID jobId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        ImportJob job = ImportJob.create(tenantId, ImportJobType.STUDENT_BULK_IMPORT, 1);
        job.setRawData(List.of(Map.of("firstName", "Jane")));

        when(importJobRepository.findByIdAndTenantId(jobId, tenantId)).thenReturn(Optional.of(job));

        OnboardingServiceImpl service = new OnboardingServiceImpl(
                importJobRepository,
                mock(CsvValidationService.class),
                mock(RabbitTemplate.class),
                mock(AuditService.class),
                new com.fasterxml.jackson.databind.ObjectMapper(),
                mock(ApplicationEventPublisher.class)
        );

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.downloadCompletionReportCsv(jobId, tenantId)
        );

        assertEquals("Completion report is only available after the import job finishes.", ex.getMessage());
    }

    @Test
    void requeueJob_throwsValidationExceptionWhenJobIsNotFailed() {
        ImportJobRepository importJobRepository = mock(ImportJobRepository.class);
        UUID jobId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        ImportJob job = ImportJob.create(tenantId, ImportJobType.STAFF_BULK_IMPORT, 3);
        job.setStatus(ImportJobStatus.DONE);

        when(importJobRepository.findByIdAndTenantId(jobId, tenantId)).thenReturn(Optional.of(job));

        OnboardingServiceImpl service = new OnboardingServiceImpl(
                importJobRepository,
                mock(CsvValidationService.class),
                mock(RabbitTemplate.class),
                mock(AuditService.class),
                new com.fasterxml.jackson.databind.ObjectMapper(),
                mock(ApplicationEventPublisher.class)
        );

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.requeueJob(jobId, tenantId)
        );

        assertEquals("Only FAILED jobs can be re-queued. Current status: DONE", ex.getMessage());
    }

    @Test
    void deleteJob_throwsValidationExceptionWhenJobIsProcessing() {
        ImportJobRepository importJobRepository = mock(ImportJobRepository.class);
        UUID jobId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        ImportJob job = ImportJob.create(tenantId, ImportJobType.STAFF_BULK_IMPORT, 3);
        job.setStatus(ImportJobStatus.PROCESSING);

        when(importJobRepository.findByIdAndTenantId(jobId, tenantId)).thenReturn(Optional.of(job));

        OnboardingServiceImpl service = new OnboardingServiceImpl(
                importJobRepository,
                mock(CsvValidationService.class),
                mock(RabbitTemplate.class),
                mock(AuditService.class),
                new com.fasterxml.jackson.databind.ObjectMapper(),
                mock(ApplicationEventPublisher.class)
        );

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.deleteJob(jobId, tenantId)
        );

        assertEquals("Cannot delete a job that is currently PROCESSING.", ex.getMessage());
    }
}
