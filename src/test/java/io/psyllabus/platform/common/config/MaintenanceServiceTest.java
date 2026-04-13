package io.psyllabus.platform.common.config;

import io.psyllabus.platform.onboarding.repository.ImportJobRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MaintenanceServiceTest {

    @Test
    void clearStaleImportRawData_delegatesToRetentionQuery() {
        ImportJobRepository importJobRepository = mock(ImportJobRepository.class);
        when(importJobRepository.clearStaleRawData(any())).thenReturn(2);

        MaintenanceService maintenanceService = new MaintenanceService(importJobRepository);

        maintenanceService.clearStaleImportRawData();

        verify(importJobRepository).clearStaleRawData(any());
    }
}
