package io.psyllabus.platform.assignment.entities;

import io.psyllabus.platform.assignment.enums.AssignmentStatus;
import io.psyllabus.platform.assignment.enums.AssignmentType;
import io.psyllabus.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "assignments", indexes = {
        @Index(name = "idx_assignment_tenant", columnList = "tenant_id"),
        @Index(name = "idx_assignment_class_term", columnList = "class_id,academic_term_id"),
        @Index(name = "idx_assignment_staff", columnList = "staff_id"),
        @Index(name = "idx_assignment_status", columnList = "tenant_id,status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment extends Auditable {

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

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "instructions")
    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false, length = 20)
    private AssignmentType assignmentType;

    @Column(name = "max_score", nullable = false)
    @Builder.Default
    private Integer maxScore = 100;

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.DRAFT;
}
