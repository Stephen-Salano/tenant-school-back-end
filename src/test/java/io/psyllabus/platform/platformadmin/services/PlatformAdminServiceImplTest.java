package io.psyllabus.platform.platformadmin.services;

import io.psyllabus.platform.onboarding.enums.ImportJobStatus;
import io.psyllabus.platform.onboarding.repository.ImportJobRepository;
import io.psyllabus.platform.platformadmin.dto.responses.PlatformKpiResponse;
import io.psyllabus.platform.platformadmin.repositories.PlatformAdminRepository;
import io.psyllabus.platform.school.repository.SchoolRepository;
import io.psyllabus.platform.school.service.SchoolService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PlatformAdminServiceImplTest {

    @Test
    void getDashboardKpis_usesCountQueriesForImportJobs() {
        SchoolRepository schoolRepository = mock(SchoolRepository.class);
        ImportJobRepository importJobRepository = mock(ImportJobRepository.class);
        PlatformAdminRepository platformAdminRepository = mock(PlatformAdminRepository.class);

        PlatformAdminServiceImpl service = new PlatformAdminServiceImpl(
                mock(SchoolService.class),
                schoolRepository,
                importJobRepository,
                platformAdminRepository,
                mock(org.springframework.security.crypto.password.PasswordEncoder.class)
        );

        when(schoolRepository.count()).thenReturn(12L);
        when(schoolRepository.countByIsActiveTrue()).thenReturn(9L);
        when(importJobRepository.countByStatus(ImportJobStatus.QUEUED)).thenReturn(4L);
        when(importJobRepository.countByStatus(ImportJobStatus.PROCESSING)).thenReturn(2L);
        when(importJobRepository.countByStatus(ImportJobStatus.FAILED)).thenReturn(1L);

        PlatformKpiResponse response = service.getDashboardKpis();

        assertEquals(12L, response.totalSchools());
        assertEquals(9L, response.activeSchools());
        assertEquals(3L, response.inactiveSchools());
        assertEquals(4L, response.queuedImportJobs());
        assertEquals(2L, response.processingImportJobs());
        assertEquals(1L, response.failedImportJobs());

        verify(importJobRepository).countByStatus(ImportJobStatus.QUEUED);
        verify(importJobRepository).countByStatus(ImportJobStatus.PROCESSING);
        verify(importJobRepository).countByStatus(ImportJobStatus.FAILED);
        verify(importJobRepository, never()).findAllByStatus(any());
    }
}
