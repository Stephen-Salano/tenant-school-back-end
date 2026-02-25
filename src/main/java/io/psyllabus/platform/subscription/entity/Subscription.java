package io.psyllabus.platform.subscription.entity;

import io.psyllabus.platform.common.audit.Auditable;
import io.psyllabus.platform.subscription.enums.PlanType;
import io.psyllabus.platform.subscription.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Subscription entity for plan-based feature gating.
 * Each school (tenant) has a subscription that determines available features.
 */
@Entity
@Table(name = "subscriptions", indexes = {
        @Index(name = "idx_subscriptions_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_plan_type", columnList = "plan_type"),
        @Index(name = "idx_is_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 50)
    private PlanType planType;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

//    @Column(name = "max_students")
//    private Integer maxStudents;
//
//    @Column(name = "max_branches")
//    private Integer maxBranches;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features", columnDefinition = "jsonb")
    private List<String> features;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Check if a specific feature is enabled in this subscription
     *
     * @param featureName the feature name to check (e.g., teacher_ai)
     * @return true if feature is enabled, false otherwise
     */
    public boolean hasFeature(String featureName){
        return features != null && features.contains(featureName);
    }

    /**
     * Check if subscription is currently valid
     *
     * @return true if active and not expired, false otherwise
     */
    public boolean isValid(){
        if (!isActive || (status != SubscriptionStatus.ACTIVE)){
            return false;
        }
        if (endDate == null){
            return true; // No expiry date => valid
        }

        return LocalDateTime.now().isBefore(endDate);
    }
}
