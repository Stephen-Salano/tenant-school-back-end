package io.psyllabus.platform.audit.service;

import io.psyllabus.platform.audit.dto.request.AuditExportRequest;
import io.psyllabus.platform.audit.dto.response.AuditExportJobResponse;
import io.psyllabus.platform.audit.entitiy.AuditExportJob;
import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.enums.AuditExportStatus;
import io.psyllabus.platform.audit.event.AuditExportQueuedEvent;
import io.psyllabus.platform.audit.repository.AuditExportJobRepository;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditExportServiceImplTest {

    @Test
    void queueExport_persistsQueuedJobAndPublishesEvent() {
        AuditExportJobRepository repository = mock(AuditExportJobRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        UUID requestingUserId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        LocalDateTime requestedAt = LocalDateTime.now();
        AuditExportRequest request = new AuditExportRequest(
                AuditAction.READ,
                "STUDENT",
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDateTime.now().minusDays(7),
                LocalDateTime.now()
        );

        when(repository.save(any(AuditExportJob.class))).thenAnswer(invocation -> {
            AuditExportJob job = invocation.getArgument(0);
            job.setId(jobId);
            job.setCreatedAt(requestedAt);
            return job;
        });

        AuditExportServiceImpl service = new AuditExportServiceImpl(repository, eventPublisher, redisTemplate);

        AuditExportJobResponse response = service.queueExport(request, requestingUserId);

        assertEquals(jobId, response.jobId());
        assertEquals(AuditExportStatus.QUEUED, response.status());
        assertEquals(requestedAt, response.requestedAt());

        verify(repository).save(any(AuditExportJob.class));
        verify(eventPublisher).publishEvent(new AuditExportQueuedEvent(jobId));
    }

    @Test
    void getJobStatus_throwsWhenRequesterDoesNotOwnJob() {
        AuditExportJobRepository repository = mock(AuditExportJobRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        UUID jobId = UUID.randomUUID();
        UUID requestingUserId = UUID.randomUUID();
        when(repository.findByIdAndRequestingUserId(jobId, requestingUserId)).thenReturn(Optional.empty());

        AuditExportServiceImpl service = new AuditExportServiceImpl(repository, eventPublisher, redisTemplate);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getJobStatus(jobId, requestingUserId)
        );

        assertEquals("Audit export job not found", exception.getMessage());
    }

    @Test
    void downloadExport_throwsWhenBlobExpired() {
        AuditExportJobRepository repository = mock(AuditExportJobRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        UUID jobId = UUID.randomUUID();
        UUID requestingUserId = UUID.randomUUID();

        AuditExportJob job = AuditExportJob.queue(requestingUserId, new AuditExportRequest(null, null, null, null, null, null));
        job.setId(jobId);
        job.setStatus(AuditExportStatus.COMPLETED);
        job.setRedisKey("audit-export:" + jobId);

        when(repository.findByIdAndRequestingUserId(jobId, requestingUserId)).thenReturn(Optional.of(job));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(job.getRedisKey())).thenReturn(null);

        AuditExportServiceImpl service = new AuditExportServiceImpl(repository, eventPublisher, redisTemplate);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.downloadExport(jobId, requestingUserId)
        );

        assertEquals("Audit export file expired or is no longer available. Request a new export.", exception.getMessage());
    }
}
