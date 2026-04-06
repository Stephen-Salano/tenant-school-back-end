package io.psyllabus.platform.academic.entities;

import io.psyllabus.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * A subject (e.g. "Mathematics", "English", "Biology") within a school's curriculum.
 *
 * <p>A subject references two classifiers:</p>
 * <ul>
 * <li>{@code subjectGroupId} → curriculum area ({@link SubjectGroup}):
 * "Languages &amp; Literacy", "Sciences", etc.</li>
 * <li>{@code subjectCategoryId} → requirement type ({@link SubjectCategory}):
 * "Core", "Elective", "Practical", etc.</li>
 * </ul>
 *
 * <p>Both are optional at creation time to allow staged onboarding — a subject can
 * be created before the admin has fully configured groups and categories.</p>
 *
 * <p>Subject code is unique within a tenant (enforced by unique constraint) and used
 * for short-form references in reports and grading sheets.</p>
 */
@Entity
@Table(name = "subjects", indexes = {
    @Index(name = "idx_subject_tenant", columnList = "tenant_id"),
    @Index(name = "idx_subject_group", columnList = "subject_group_id"),
    @Index(name = "idx_subject_category", columnList = "subject_category_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_subject_code_tenant", columnNames = {"code", "tenant_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Short code unique within the tenant. e.g. {@code "MATH"}, {@code "ENG"}. */
    @Column(name = "code", length = 20)
    private String code;

    /** FK to {@link SubjectGroup} — the curriculum area this subject belongs to. */
    @Column(name = "subject_group_id")
    private UUID subjectGroupId;

    /** FK to {@link SubjectCategory} — Core / Elective / Practical / Optional. */
    @Column(name = "subject_category_id")
    private UUID subjectCategoryId;

    /** Maximum achievable score in this subject. e.g. {@code 100}. */
    @Column(name = "max_score")
    private Integer maxScore;

    /** Minimum score to pass. e.g. {@code 50}. */
    @Column(name = "pass_mark")
    private Integer passMark;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
