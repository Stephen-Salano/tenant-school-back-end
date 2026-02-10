package io.psyllabus.platform.school.entities;

import io.psyllabus.platform.common.audit.Auditable;
import io.psyllabus.platform.school.enums.SubscriptionPlan;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "schools", indexes = {
        @Index(name = "idx_subdomain_prefix", columnList = "subdomain_prefix"),
        @Index(name = "idx_custom_domain", columnList = "custom_domain"),
        @Index(name = "idx_plan", columnList = "plan"),
        @Index(name = "idx_is_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class School extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "subdomain_prefix", unique = true, length = 50)
    private String subdomainPrefix;

    @Column(name = "custom_domain", unique = true, length = 100)
    private String customDomain;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", length = 20)
    @Builder.Default
    private SubscriptionPlan plan = SubscriptionPlan.BASIC;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

}
