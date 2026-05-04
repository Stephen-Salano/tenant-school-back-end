package io.psyllabus.platform.onboarding.entity;

import io.psyllabus.platform.onboarding.dto.ImportErrorDetail;
import io.psyllabus.platform.onboarding.dto.ImportRowOutcome;
import io.psyllabus.platform.onboarding.enums.ImportJobType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ImportJobTest {

    @Test
    void recordBatchResult_shouldReassignListsToTriggerDirtyChecking() {
        ImportJob job = ImportJob.create(UUID.randomUUID(), ImportJobType.STUDENT_BULK_IMPORT, 10);
        List<ImportErrorDetail> initialErrors = job.getErrorDetails();
        List<ImportRowOutcome> initialOutcomes = job.getRowOutcomes();

        List<ImportErrorDetail> batchErrors = List.of(new ImportErrorDetail(1, "phone", "Invalid", "123"));
        List<ImportRowOutcome> batchOutcomes = List.of(new ImportRowOutcome(1, "FAILED", "Row 1", "Invalid phone"));

        job.recordBatchResult(0, batchErrors, batchOutcomes);

        assertNotSame(initialErrors, job.getErrorDetails(), "Error list should be a new instance");
        assertNotSame(initialOutcomes, job.getRowOutcomes(), "Outcome list should be a new instance");
        assertEquals(1, job.getErrorDetails().size());
        assertEquals(1, job.getRowOutcomes().size());
        assertEquals(1, job.getProcessedRows());
    }

    @Test
    void appendOutcomes_shouldOnlyUpdateListsAndReassignInstances() {
        ImportJob job = ImportJob.create(UUID.randomUUID(), ImportJobType.STAFF_BULK_IMPORT, 5);
        int initialProcessed = job.getProcessedRows();
        List<ImportErrorDetail> initialErrors = job.getErrorDetails();

        List<ImportErrorDetail> batchErrors = List.of(new ImportErrorDetail(1, "email", "Duplicate", "a@b.com"));
        List<ImportRowOutcome> batchOutcomes = List.of(new ImportRowOutcome(1, "FAILED", "a@b.com", "Duplicate"));

        job.appendOutcomes(batchErrors, batchOutcomes);

        assertNotSame(initialErrors, job.getErrorDetails(), "Error list should be a new instance");
        assertEquals(initialProcessed, job.getProcessedRows(), "Processed rows counter should not change");
        assertEquals(1, job.getErrorDetails().size());
    }
}
