package io.psyllabus.platform.onboarding.repository;

import io.psyllabus.platform.onboarding.entity.ImportBatchOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ImportBatchOutcomeRepository extends JpaRepository<ImportBatchOutcome, UUID> {
    List<ImportBatchOutcome> findByJobId(UUID jobId);
}
