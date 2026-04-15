package io.psyllabus.platform.academic.entities;

import io.psyllabus.platform.academic.enums.AcademicStatus;
import io.psyllabus.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * A class (section) within a school for a given academic year.
 *
 * <p>Mental model: "Grade 1 Blue" is modelled as
 * {@code SchoolClass(name="Grade 1", gradeLevel="Grade 1", streamName="Blue")}.
 * The school admin first configures valid stream names in
 * {@code SchoolProfile.streamNames} (e.g. {@code ["Blue", "Red", "Green"]}).
 * {@code ClassServiceImpl.createClass()} validates the incoming {@code streamName}
 * against that list — prevents arbitrary free-text that breaks filtering later.</p>
 *
 * <p>If {@code SchoolProfile.streamNames} is null or empty, stream name validation
 * is skipped to avoid blocking schools that haven't yet configured their profile.</p>
 *
 * <p>A class is scoped to a single academic year. New year → new class records.</p>
 */
@Entity
@Table(name = "school_classes", indexes = {
    @Index(name = "idx_school_class_tenant", columnList = "tenant_id"),
    @Index(name = "idx_school_class_year", columnList = "academic_year_id"),
    @Index(name = "idx_school_class_branch", columnList = "branch_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolClass extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Grade label. e.g. {@code "Grade 1"}, {@code "Form 2"}. */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Normalised grade level for sorting/filtering. e.g. {@code "Grade 1"}. */
    @Column(name = "grade_level", length = 50)
    private String gradeLevel;

    /**
     * Stream name validated against {@code SchoolProfile.streamNames}.
     * e.g. {@code "Blue"}, {@code "North"}, {@code "Lion"}.
     * {@code null} for schools that do not use streams.
     */
    @Column(name = "stream_name", length = 50)
    private String streamName;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    /** Optional: restricts this class to a specific branch. */
    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AcademicStatus status = AcademicStatus.ACTIVE;
}
