package io.psyllabus.platform.academic.entities;

import io.psyllabus.platform.academic.enums.AssignmentStatus;
import io.psyllabus.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * The SM3 convergence point — answers: <em>who teaches which subject, in which class,
 * during which term?</em>
 *
 * <p>A teacher can hold multiple assignments simultaneously:</p>
 * <pre>
 * Ms. Jane → English → Grade 1 Blue → Term 1 2026 (record A)
 * Ms. Jane → Swahili → Grade 3 Red → Term 1 2026 (record B)
 * </pre>
 *
 * <p>The unique constraint {@code (staff_id, class_id, subject_id, academic_term_id)}
 * prevents duplicate assignments at the DB level. The service layer checks this
 * constraint before inserting and returns HTTP 409 on conflict with a clear message.</p>
 *
 * <p>Class teacher flag: {@code isClassTeacher = true} identifies the homeroom teacher.
 * Only one teacher per class can hold this flag at a time.
 * {@code ClassServiceImpl.assignClassTeacher()} demotes the previous class teacher
 * (sets their flag to false) before assigning the new one.</p>
 *
 * <p>Soft delete via {@link AssignmentStatus#INACTIVE} — keeps the row for historical
 * timetable and reporting queries. Use {@code status = INACTIVE} instead of deleting.</p>
 */
@Entity
@Table(name = "teacher_assignments", indexes = {
    @Index(name = "idx_teacher_assignment_tenant", columnList = "tenant_id"),
    @Index(name = "idx_teacher_assignment_staff", columnList = "staff_id"),
    @Index(name = "idx_teacher_assignment_class", columnList = "class_id"),
    @Index(name = "idx_teacher_assignment_term", columnList = "academic_term_id")
}, uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_teacher_assignment",
        columnNames = {"staff_id", "class_id", "subject_id", "academic_term_id", "tenant_id"}
    )
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherAssignment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "academic_term_id", nullable = false)
    private UUID academicTermId;

    /**
     * Homeroom / class teacher flag. Only one teacher per class should have this set to
     * {@code true}. {@code ClassServiceImpl.assignClassTeacher()} manages this atomically.
     */
    @Column(name = "is_class_teacher", nullable = false)
    @Builder.Default
    private boolean isClassTeacher = false;

    /** How many lessons per week this teacher delivers for this assignment. */
    @Column(name = "lessons_per_week")
    private Integer lessonsPerWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.ACTIVE;
}
