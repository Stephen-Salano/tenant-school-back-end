package io.psyllabus.platform.assignment.entities;

import io.psyllabus.platform.assignment.enums.SubmissionStatus;
import io.psyllabus.platform.assignment.enums.SubmissionType;
import io.psyllabus.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "assignment_submissions", indexes = {
        @Index(name = "idx_submission_assignment", columnList = "assignment_id,tenant_id"),
        @Index(name = "idx_submission_student", columnList = "student_id,tenant_id"),
        @Index(name = "idx_submission_status", columnList = "tenant_id,submission_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentSubmission extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_status", nullable = false, length = 20)
    @Builder.Default
    private SubmissionStatus submissionStatus = SubmissionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_type", length = 20)
    private SubmissionType submissionType;

    @Column(name = "text_content")
    private String textContent;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "is_late", nullable = false)
    @Builder.Default
    private boolean isLate = false;

    @Column(name = "score")
    private Integer score;

    @Column(name = "grade_label", length = 120)
    private String gradeLabel;

    @Column(name = "feedback")
    private String feedback;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @Column(name = "graded_by")
    private UUID gradedBy;
}
