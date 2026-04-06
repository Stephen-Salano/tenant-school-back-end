package io.psyllabus.platform.academic.entities;

import io.psyllabus.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * Curriculum-neutral grouping of subjects.
 *
 * <p>Replaces the legacy {@code Department} entity. Instead of tying groups to an
 * organisational structure (e.g. "Science Department"), {@code SubjectGroup} models
 * the <em>curriculum area</em> a subject belongs to:</p>
 *
 * <ul>
 * <li>CBC: Languages &amp; Literacy, Mathematics, Science &amp; Technology, etc.</li>
 * <li>IGCSE: Sciences, Humanities, Languages, Arts, etc.</li>
 * </ul>
 *
 * <p>Each school configures its own groups — a school on CBC might seed the 7
 * standard Learning Areas; an IGCSE school builds its own groupings.
 * Subject groups are tenant-scoped and never shared across schools.</p>
 *
 * <p>HOD assignment is optional: a group may exist without a head of department
 * during the initial setup wizard phase.</p>
 */
@Entity
@Table(name = "subject_groups", indexes = {
    @Index(name = "idx_subject_group_tenant", columnList = "tenant_id"),
    @Index(name = "idx_subject_group_hod", columnList = "hod_staff_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectGroup extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /**
     * Display name for this curriculum area.
     * e.g. "Languages &amp; Literacy", "Sciences", "Mathematics".
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Optional: UUID of the Staff record appointed as Head of Department for this group.
     * Validated at the service layer — must belong to the same tenant.
     * {@code null} until an admin assigns a HOD.
     */
    @Column(name = "hod_staff_id")
    private UUID hodStaffId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
