package io.psyllabus.platform.tenant.secret.entity;

import io.psyllabus.platform.common.audit.Auditable;
import io.psyllabus.platform.tenant.secret.enums.SecretType;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "tenant_secrets", indexes = {
    @Index(name = "idx_tenant_secrets_tenant", columnList = "tenant_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uq_tenant_secrets_type", columnNames = {"tenant_id", "secret_type"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantSecret extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "secret_type", nullable = false, length = 50)
    private SecretType secretType;

    @Column(name = "secret_value", nullable = false)
    private String secretValue;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
