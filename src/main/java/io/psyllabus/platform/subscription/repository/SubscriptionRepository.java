package io.psyllabus.platform.subscription.repository;

import io.psyllabus.platform.subscription.entity.Subscription;
import io.psyllabus.platform.subscription.enums.PlanType;
import io.psyllabus.platform.subscription.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
/**
 * Repository for Subscription entity.
 * Manages plan-based feature gating and subscription lifecycle.
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    /**
     * Find active subscription for a tenant.
     * CRITICAL: Used by SubscriptionInterceptor to check feature access.
     * Result should be cached in Redis (5-minute TTL).
     *
     * @param tenantId the tenant (school) ID
     * @return Optional containing the active subscription if found
     */
    Optional<Subscription> findByTenantIdAndIsActiveTrue(UUID tenantId);

    /**
     * Find all subscriptions for a tenant (including inactive).
     * Used for subscription history and admin purposes.
     *
     * @param tenantId the tenant (school) ID
     * @return list of subscriptions
     */
    List<Subscription> findByTenantId(UUID tenantId);

    @Query("""
    SELECT
      s
    FROM
      Subscription s
    WHERE
      s.isActive = TRUE
      AND s.endDate BETWEEN:fromDate AND: toDate
      AND s.status = 'ACTIVE'
    """)
    List<Subscription> findExpiringSoon(
            @Param("fromDate")LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
            );

    /**
     * Count subscriptions by plan type
     * Used for business analytics
     * @param planType the plan type
     * @return count of subscriptions
     */
    long countByPlanType(PlanType planType);

    /**
     * Count active subscriptions
     * Used for business metrics
     * @param status the plan type status active?
     * @return count of active subscriptions
     */
    long countByIsActiveTrueAndStatus(SubscriptionStatus status);

    /**
     * Find subscription by tenant and plan type
     * Used during upgrades/downgrades
     *
     * @param tenantId the tenant id
     * @param planType the plan type
     * @return Optional containing the subscription if found
     */
    Optional<Subscription> findByTenantIdAndPlanType(UUID tenantId, PlanType planType);
}
