package io.psyllabus.platform.common.config;

import io.psyllabus.platform.academic.repository.AcademicTermRepository;
import io.psyllabus.platform.attendance.repository.AttendanceRepository;
import io.psyllabus.platform.onboarding.repository.ImportJobRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MaintenanceServiceTest {

    @Test
    void clearStaleImportRawData_delegatesToRetentionQuery() {
        ImportJobRepository importJobRepository = mock(ImportJobRepository.class);
        AttendanceRepository attendanceRepository = mock(AttendanceRepository.class);
        AcademicTermRepository academicTermRepository = mock(AcademicTermRepository.class);
        when(importJobRepository.clearStaleRawData(any())).thenReturn(2);

        MaintenanceService maintenanceService = new MaintenanceService(
                importJobRepository, attendanceRepository, academicTermRepository);

        maintenanceService.clearStaleImportRawData();

        verify(importJobRepository).clearStaleRawData(any());
    }
}