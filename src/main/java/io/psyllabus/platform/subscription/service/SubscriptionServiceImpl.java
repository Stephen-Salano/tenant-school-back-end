package io.psyllabus.platform.subscription.service;

import io.psyllabus.platform.subscription.entity.Subscription;
import io.psyllabus.platform.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    @Override
    @Cacheable(value = "subscriptions", key = "#tenantId", unless = "#result == null")
    public Subscription getActiveSubscription(UUID tenantId) {
        log.debug("Loading subscription for tenant: {} (cache miss)", tenantId);
        return subscriptionRepository.findByTenantIdAndIsActiveTrue(tenantId)
                .orElse(null);
    }
}
