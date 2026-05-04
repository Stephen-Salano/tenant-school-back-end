package io.psyllabus.platform.platformadmin.services;

import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.onboarding.enums.ImportJobStatus;
import io.psyllabus.platform.onboarding.repository.ImportJobRepository;
import io.psyllabus.platform.platformadmin.dto.responses.PlatformKpiResponse;
import io.psyllabus.platform.platformadmin.repositories.PlatformAdminRepository;
import io.psyllabus.platform.school.enums.SchoolStatus;
import io.psyllabus.platform.school.repository.SchoolRepository;
import io.psyllabus.platform.school.service.SchoolService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PlatformAdminServiceImplTest {

    @Test
    void getDashboardKpis_usesCountQueriesForImportJobs() {
        SchoolRepository schoolRepository = mock(SchoolRepository.class);
        ImportJobRepository importJobRepository = mock(ImportJobRepository.class);
        PlatformAdminRepository platformAdminRepository = mock(PlatformAdminRepository.class);
        AuditService auditService = mock(AuditService.class);

        PlatformAdminServiceImpl service = new PlatformAdminServiceImpl(
                mock(SchoolService.class),
                schoolRepository,
                importJobRepository,
                platformAdminRepository,
                mock(PasswordEncoder.class),
                auditService
        );

        when(schoolRepository.count()).thenReturn(15L);
        when(schoolRepository.countByStatus(SchoolStatus.ACTIVE)).thenReturn(10L);
        when(schoolRepository.countByStatus(SchoolStatus.SUSPENDED)).thenReturn(3L);
        when(schoolRepository.countByStatus(SchoolStatus.DEACTIVATED)).thenReturn(2L);
        when(importJobRepository.countByStatus(ImportJobStatus.QUEUED)).thenReturn(4L);
        when(importJobRepository.countByStatus(ImportJobStatus.PROCESSING)).thenReturn(2L);
        when(importJobRepository.countByStatus(ImportJobStatus.FAILED)).thenReturn(1L);

        PlatformKpiResponse response = service.getDashboardKpis();

        assertEquals(15L, response.totalSchools());
        assertEquals(10L, response.activeSchools());
        assertEquals(3L, response.suspendedSchools());
        assertEquals(2L, response.deactivatedSchools());
        assertEquals(4L, response.queuedImportJobs());
        assertEquals(2L, response.processingImportJobs());
        assertEquals(1L, response.failedImportJobs());

        verify(importJobRepository).countByStatus(ImportJobStatus.QUEUED);
        verify(importJobRepository).countByStatus(ImportJobStatus.PROCESSING);
        verify(importJobRepository).countByStatus(ImportJobStatus.FAILED);
    }
}
