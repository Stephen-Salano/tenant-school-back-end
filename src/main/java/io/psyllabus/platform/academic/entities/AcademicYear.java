package io.psyllabus.platform.academic.entities;

import io.psyllabus.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A school's academic year — the top-level calendar container.
 *
 * <p>Terms, classes, and assignments are all scoped to an academic year.
 * Every school must have exactly one {@code isCurrent = true} year at any time.
 * The {@code @Version} field enables optimistic locking on
 * {@code AcademicYearServiceImpl.setCurrentYear()} to prevent two concurrent
 * admin tabs from setting two different years as current simultaneously — only
 * the first write wins; the second gets a {@code ObjectOptimisticLockingFailureException}.</p>
 *
 * <p>A partial unique index {@code (tenant_id, is_current) WHERE is_current = true}
 * enforces this constraint at the DB level. The Flyway migration (or Hibernate in dev)
 * creates this index.</p>
 */
@Entity
@Table(name = "academic_years", indexes = {
    @Index(name = "idx_academic_year_tenant", columnList = "tenant_id"),
    @Index(name = "idx_academic_year_current", columnList = "tenant_id,is_current")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicYear extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Human-readable label. e.g. {@code "2026"} or {@code "2025/2026"}. */
    @Column(name = "name", nullable = false, length = 20)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * Only one academic year per tenant should be current at any time.
     * Guarded by optimistic locking (see {@code version}) and a DB partial unique index.
     */
    @Column(name = "is_current", nullable = false)
    @Builder.Default
    private boolean isCurrent = false;

    /**
     * Optimistic locking version counter.
     * {@code AcademicYearServiceImpl.setCurrentYear()} flips {@code isCurrent} flags.
     * Without this, two concurrent requests could both set their respective years
     * as current, leaving two rows with {@code isCurrent = true}.
     */
    @Version
    @Column(name = "version")
    private Long version;
}
