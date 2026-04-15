package io.psyllabus.platform.branch.entities;

import io.psyllabus.platform.branch.enums.BranchStatus;
import io.psyllabus.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "branches", indexes = {
        @Index(name = "idx_branches_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_tenant_code", columnList = "tenant_id,code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "code", nullable = false, length = 20)
    private String code;  // e.g., "MAIN", "BRANCH-A"

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "branch_admin_id")
    private UUID branchAdminId;  // References users.id

    @Column(name = "is_main_branch", nullable = false)
    @Builder.Default
    private boolean isMainBranch = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BranchStatus status = BranchStatus.ACTIVE;
}
