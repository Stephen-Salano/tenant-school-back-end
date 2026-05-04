package io.psyllabus.platform.grading.entities;

import io.psyllabus.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "grading_levels", indexes = {
        @Index(name = "idx_grading_level_scheme", columnList = "scheme_id,tenant_id"),
        @Index(name = "idx_grading_level_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingLevel extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "scheme_id", nullable = false)
    private UUID schemeId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "min_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal minPercentage;

    @Column(name = "max_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxPercentage;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "description")
    private String description;
}
