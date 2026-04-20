package io.psyllabus.platform.onboarding.entity;

import io.psyllabus.platform.common.audit.Auditable;
import io.psyllabus.platform.onboarding.dto.ImportErrorDetail;
import io.psyllabus.platform.onboarding.dto.ImportRowOutcome;
import io.psyllabus.platform.onboarding.enums.ImportJobStatus;
import io.psyllabus.platform.onboarding.enums.ImportJobType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tracks a single bulk-import job from QUEUED through to completion.
 *
 * <p>{@code errorDetails} is stored as a JSONB array — no join table needed.
 * {@code rawData} holds the parsed CSV rows so S3 is not required.
 * Both are sized well within Postgres limits for ≤ 500 rows (~150 KB). </p>
 */
@Entity
@Table(
        name = "import_jobs",
        indexes = {
                @Index(name = "idx_import_jobs_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_import_jobs_status", columnList = "status"),
                @Index(name = "idx_import_jobs_tenant_created", columnList = "tenant_id, created_at DESC")
        }
)
@Getter
@Setter
@NoArgsConstructor
@org.hibernate.annotations.DynamicUpdate
public class ImportJob extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 30)
    private ImportJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ImportJobStatus status = ImportJobStatus.QUEUED;

    @Column(name = "total_rows")
    private int totalRows;

    @Column(name = "processed_rows")
    private int processedRows;

    @Column(name = "success_rows")
    private int successRows;

    @Column(name = "error_rows")
    private int errorRows;

    /** JSONB array of per-row errors. Appended per batch; never rewritten wholesale. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "error_details", columnDefinition = "jsonb")
    private List<ImportErrorDetail> errorDetails = new ArrayList<>();

    /** JSONB array of per-row outcomes (success/fail with final identifiers). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "row_outcomes", columnDefinition = "jsonb")
    private List<ImportRowOutcome> rowOutcomes = new ArrayList<>();

    /**
     * Full parsed CSV stored as JSONB to avoid S3 costs.
     * Retained briefly after completion so admins can download the completion report,
     * then nulled by MaintenanceService after the retention window elapses.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    private List<Object> rawData = new ArrayList<>(); // List<Map<String,String>> at runtime

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PostLoad
    public void ensureCollectionsInitialized() {
        if (this.errorDetails == null) this.errorDetails = new ArrayList<>();
        if (this.rowOutcomes == null) this.rowOutcomes = new ArrayList<>();
        if (this.rawData == null) this.rawData = new ArrayList<>();
    }

    // ── Factory ──────────────────────────────────────────────────────────────

    /**
     * Create a new job in QUEUED state.
     *
     * @param tenantId the school initiating the import
     * @param jobType  STUDENT_BULK_IMPORT or STAFF_BULK_IMPORT
     * @param totalRows total CSV data rows (excluding header)
     */
    public static ImportJob create(UUID tenantId, ImportJobType jobType, int totalRows) {
        ImportJob job = new ImportJob();
        job.setTenantId(tenantId);
        job.setJobType(jobType);
        job.setTotalRows(totalRows);
        job.setStatus(ImportJobStatus.QUEUED);
        return job;
    }

    // ── Mutators used by listeners ───────────────────────────────────────────

    /** Mark the job as started; called by the listener before processing the first batch. */
    public void markProcessing() {
        this.status = ImportJobStatus.PROCESSING;
    }

    /**
     * Append errors and outcomes from a just-processed batch and advance the progress counters.
     *
     * @param batchSuccesses number of rows persisted successfully in this batch
     * @param batchErrors     errors encountered in this batch
     * @param batchOutcomes   outcomes (success/fail) for all rows in the batch
     */
    public void recordBatchResult(int batchSuccesses, List<ImportErrorDetail> batchErrors, List<ImportRowOutcome> batchOutcomes) {
        this.successRows += batchSuccesses;
        this.errorRows += batchErrors.size();
        this.processedRows += batchSuccesses + batchErrors.size(); // key line: checkpoint

        if (this.errorDetails == null) this.errorDetails = new ArrayList<>();
        this.errorDetails.addAll(batchErrors);

        if (this.rowOutcomes == null) this.rowOutcomes = new ArrayList<>();
        this.rowOutcomes.addAll(batchOutcomes);
    }

    /** Transition to a terminal status and stamp completion time. */
    public void complete() {
        this.status = errorRows > 0 ? ImportJobStatus.COMPLETED_WITH_ERRORS : ImportJobStatus.DONE;
        this.completedAt = LocalDateTime.now();
    }

    /** Mark the job as permanently failed (e.g. unparseable CSV, fatal DB error). */
    public void fail(String reason) {
        this.status = ImportJobStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        if (this.errorDetails == null) this.errorDetails = new ArrayList<>();
        this.errorDetails.add(new ImportErrorDetail(0, null, reason, null));
    }
}
