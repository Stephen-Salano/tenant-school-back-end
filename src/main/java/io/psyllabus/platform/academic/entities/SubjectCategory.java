package io.psyllabus.platform.academic.entities;

import io.psyllabus.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * Type classifier for a subject — its requirement status within the curriculum.
 *
 * <p>Examples: Core, Elective, Practical, Optional.
 * This is distinct from {@link SubjectGroup}, which answers <em>which area</em>
 * a subject belongs to. {@code SubjectCategory} answers <em>what kind</em> it is.</p>
 *
 * <p>A subject references both:
 * {@code subjectGroupId} → "Languages &amp; Literacy"
 * {@code subjectCategoryId} → "Core"</p>
 *
 * <p>Tenant-scoped — each school defines its own categories.</p>
 */
@Entity
@Table(name = "subject_categories", indexes = {
    @Index(name = "idx_subject_category_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectCategory extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /**
     * Category label. e.g. "Core", "Elective", "Practical", "Optional".
     */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
