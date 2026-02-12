package io.psyllabus.platform.subscription.service;

import io.psyllabus.platform.subscription.entity.Subscription;

import java.util.UUID;

public interface SubscriptionService {

    /**
     * Get the active subscription for a tenant.
     * Results are cached to avoid frequent DB lookups.
     *
     * @param tenantId the tenant UUID
     * @return the active subscription, or null if none found
     */
    Subscription getActiveSubscription(UUID tenantId);
}
