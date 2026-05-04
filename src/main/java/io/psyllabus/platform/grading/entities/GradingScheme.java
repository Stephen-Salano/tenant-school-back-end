package io.psyllabus.platform.grading.entities;

import io.psyllabus.platform.common.audit.Auditable;
import io.psyllabus.platform.grading.enums.GradingSystem;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "grading_schemes", indexes = {
        @Index(name = "idx_grading_scheme_tenant", columnList = "tenant_id"),
        @Index(name = "idx_grading_scheme_default", columnList = "tenant_id,is_default")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingScheme extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "grading_system", nullable = false, length = 30)
    private GradingSystem gradingSystem;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
