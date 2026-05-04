package io.psyllabus.platform.academic.entities;

import io.psyllabus.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Junction table tracking student membership in classes.
 *
 * <p>While {@code Student.currentClassId} exists for display convenience, 
 * {@code StudentClassEnrollment} is the canonical source of truth for 
 * many-to-many and historical enrollment data.</p>
 */
@Entity
@Table(
    name = "student_class_enrollments",
    indexes = {
        @Index(name = "idx_enrollment_tenant", columnList = "tenant_id"),
        @Index(name = "idx_enrollment_class_tenant", columnList = "class_id, tenant_id"),
        @Index(name = "idx_enrollment_student_tenant", columnList = "student_id, tenant_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_student_class_enrollment", 
            columnNames = {"student_id", "class_id", "tenant_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentClassEnrollment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "academic_year_id")
    private UUID academicYearId;

    @Column(name = "enrolled_at")
    @Builder.Default
    private LocalDateTime enrolledAt = LocalDateTime.now();

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
