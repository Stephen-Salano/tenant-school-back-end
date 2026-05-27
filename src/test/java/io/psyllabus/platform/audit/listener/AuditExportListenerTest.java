package io.psyllabus.platform.audit.listener;

import io.psyllabus.platform.audit.dto.request.AuditExportRequest;
import io.psyllabus.platform.audit.dto.response.AuditLogResponse;
import io.psyllabus.platform.audit.entitiy.AuditExportJob;
import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.enums.AuditExportStatus;
import io.psyllabus.platform.audit.repository.AuditExportJobRepository;
import io.psyllabus.platform.audit.service.AuditQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuditExportListenerTest {

    @Test
    void handle_completesQueuedJobAndStoresCsvInRedis() {
        AuditExportJobRepository repository = mock(AuditExportJobRepository.class);
        AuditQueryService auditQueryService = mock(AuditQueryService.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        UUID jobId = UUID.randomUUID();
        UUID requestingUserId = UUID.randomUUID();
        AuditExportRequest criteria = new AuditExportRequest(AuditAction.READ, "STUDENT", null, null, null, null);
        AuditExportJob job = AuditExportJob.queue(requestingUserId, criteria);
        job.setId(jobId);

        when(repository.findById(jobId)).thenReturn(Optional.of(job));
        when(repository.save(any(AuditExportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditQueryService.listPlatformLogsForExport(criteria)).thenReturn(List.of(
                new AuditLogResponse(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Jane Doe",
                        AuditAction.READ,
                        "STUDENT",
                        UUID.randomUUID(),
                        "Viewed record",
                        "127.0.0.1",
                        LocalDateTime.now()
                )
        ));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AuditExportListener listener = new AuditExportListener(repository, auditQueryService, redisTemplate);

        listener.handle(new AuditExportJobPayload(jobId));

        assertEquals(AuditExportStatus.COMPLETED, job.getStatus());
        assertEquals(1, job.getTotalRecords());
        assertEquals("audit-export:" + jobId, job.getRedisKey());
        assertNotNull(job.getCompletedAt());

        verify(valueOperations).set(eq("audit-export:" + jobId), contains("Jane Doe"), eq(Duration.ofHours(24)));
    }

    @Test
    void handle_marksJobFailedWithSanitizedReasonWhenProcessingThrows() {
        AuditExportJobRepository repository = mock(AuditExportJobRepository.class);
        AuditQueryService auditQueryService = mock(AuditQueryService.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        UUID jobId = UUID.randomUUID();
        UUID requestingUserId = UUID.randomUUID();
        AuditExportRequest criteria = new AuditExportRequest(AuditAction.READ, "STUDENT", null, null, null, null);
        AuditExportJob job = AuditExportJob.queue(requestingUserId, criteria);
        job.setId(jobId);

        when(repository.findById(jobId)).thenReturn(Optional.of(job));
        when(repository.save(any(AuditExportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditQueryService.listPlatformLogsForExport(criteria))
                .thenThrow(new IllegalStateException("boom\nwith stack details"));

        AuditExportListener listener = new AuditExportListener(repository, auditQueryService, redisTemplate);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> listener.handle(new AuditExportJobPayload(jobId))
        );

        assertEquals("boom\nwith stack details", exception.getMessage());
        assertEquals(AuditExportStatus.FAILED, job.getStatus());
        assertEquals("boom with stack details", job.getFailureReason());
        assertNotNull(job.getCompletedAt());
    }

    @Test
    void handle_skipsCompletedJobsWithoutReprocessing() {
        AuditExportJobRepository repository = mock(AuditExportJobRepository.class);
        AuditQueryService auditQueryService = mock(AuditQueryService.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        UUID jobId = UUID.randomUUID();
        UUID requestingUserId = UUID.randomUUID();
        AuditExportJob job = AuditExportJob.queue(requestingUserId, new AuditExportRequest(null, null, null, null, null, null));
        job.setId(jobId);
        job.setStatus(AuditExportStatus.COMPLETED);

        when(repository.findById(jobId)).thenReturn(Optional.of(job));

        AuditExportListener listener = new AuditExportListener(repository, auditQueryService, redisTemplate);

        listener.handle(new AuditExportJobPayload(jobId));

        verifyNoInteractions(auditQueryService);
        verify(redisTemplate, never()).opsForValue();
    }
}
