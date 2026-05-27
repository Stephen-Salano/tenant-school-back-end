package io.psyllabus.platform.audit.entitiy;

import io.psyllabus.platform.audit.dto.request.AuditExportRequest;
import io.psyllabus.platform.audit.enums.AuditExportStatus;
import io.psyllabus.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "audit_export_jobs",
        indexes = {
                @Index(name = "idx_audit_export_jobs_status", columnList = "status"),
                @Index(name = "idx_audit_export_jobs_requesting_user_id", columnList = "requesting_user_id"),
                @Index(name = "idx_audit_export_jobs_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@DynamicUpdate
public class AuditExportJob extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "requesting_user_id", nullable = false)
    private UUID requestingUserId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AuditExportStatus status = AuditExportStatus.QUEUED;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "criteria", nullable = false, columnDefinition = "jsonb")
    private AuditExportRequest criteria;

    @Column(name = "total_records")
    private Integer totalRecords;

    @Column(name = "redis_key", length = 255)
    private String redisKey;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public static AuditExportJob queue(UUID requestingUserId, AuditExportRequest criteria) {
        AuditExportJob job = new AuditExportJob();
        job.setRequestingUserId(requestingUserId);
        job.setCriteria(criteria);
        job.setStatus(AuditExportStatus.QUEUED);
        return job;
    }

    public void markProcessing() {
        this.status = AuditExportStatus.PROCESSING;
        this.failureReason = null;
    }

    public void markCompleted(String redisKey, int totalRecords) {
        this.status = AuditExportStatus.COMPLETED;
        this.redisKey = redisKey;
        this.totalRecords = totalRecords;
        this.failureReason = null;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed(String failureReason) {
        this.status = AuditExportStatus.FAILED;
        this.failureReason = failureReason;
        this.completedAt = LocalDateTime.now();
    }

    public boolean isCompleted() {
        return this.status == AuditExportStatus.COMPLETED;
    }
}
