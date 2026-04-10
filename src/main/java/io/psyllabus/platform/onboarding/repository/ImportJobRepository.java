package io.psyllabus.platform.onboarding.repository;

import io.psyllabus.platform.onboarding.entity.ImportJob;
import io.psyllabus.platform.onboarding.enums.ImportJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {

    /** Tenant-scoped lookup — used by the progress-poll endpoint. */
    Optional<ImportJob> findByIdAndTenantId(UUID id, UUID tenantId);

    /** Paginated job history for a tenant, newest first. */
    Page<ImportJob> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    /** Platform-admin: cross-tenant view of jobs in a given status. */
    Page<ImportJob> findAllByStatusOrderByCreatedAtDesc(ImportJobStatus status, Pageable pageable);

    /**
     * Checkpoint query — load only progress counters, not the full JSONB payload.
     * Used by listeners to resume after a crash without pulling rawData into memory.
     */
    @Query("""
             SELECT j.processedRows FROM ImportJob j
             WHERE j.id = :id AND j.tenantId = :tenantId
           """)
    Optional<Integer> findProcessedRowsById(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    /** Find jobs stuck in PROCESSING (e.g. after a pod restart) for recovery. */
    List<ImportJob> findAllByStatus(ImportJobStatus status);

    /** Bulk-null rawData for completed jobs older than N hours — run via @Scheduled cleaner. */
    @Modifying
    @Query(value = """
             UPDATE import_jobs SET raw_data = NULL
             WHERE status IN ('DONE', 'COMPLETED_WITH_ERRORS', 'FAILED')
             AND raw_data IS NOT NULL AND completed_at < :cutoff
           """, nativeQuery = true)
    int clearStaleRawData(@Param("cutoff") LocalDateTime cutoff);
}
