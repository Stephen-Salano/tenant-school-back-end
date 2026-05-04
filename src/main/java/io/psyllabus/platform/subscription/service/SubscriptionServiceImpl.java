package io.psyllabus.platform.subscription.service;

import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.subscription.dto.request.SubscriptionCreateRequest;
import io.psyllabus.platform.subscription.dto.response.SubscriptionResponse;
import io.psyllabus.platform.subscription.entity.Subscription;
import io.psyllabus.platform.subscription.enums.SubscriptionStatus;
import io.psyllabus.platform.subscription.mapper.SubscriptionMapper;
import io.psyllabus.platform.subscription.repository.SubscriptionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMapper mapper;
    private final AuditService auditService;

    @Override
    @Cacheable(value = "subscriptions", key = "#tenantId", unless = "#result == null")
    public Subscription getActiveSubscription(UUID tenantId) {
        log.debug("Loading subscription for tenant: {} (cache miss)", tenantId);
        return subscriptionRepository.findByTenantIdAndIsActiveTrue(tenantId)
                .orElse(null);
    }

    @Override
    @Transactional
    @CacheEvict(value = "subscriptions", key = "#request.tenantId()")
    public SubscriptionResponse createSubscription(SubscriptionCreateRequest request) {
        log.info("Creating manual subscription for tenant: {}", request.tenantId());

        // Deactivate existing active subscriptions
        subscriptionRepository.findByTenantIdAndIsActiveTrue(request.tenantId())
                .ifPresent(existing -> {
                    existing.setIsActive(false);
                    existing.setStatus(SubscriptionStatus.CANCELLED);
                    subscriptionRepository.save(existing);
                    log.info("Deactivated previous subscription: {}", existing.getId());
                });

        // Create new subscription
        Subscription subscription = Subscription.builder()
                .tenantId(request.tenantId())
                .planType(request.planType())
                .startDate(request.startDate() != null ? request.startDate() : LocalDateTime.now())
                .endDate(request.endDate())
                .features(request.features())
                .status(SubscriptionStatus.ACTIVE)
                .isActive(true)
                .build();

        subscription = subscriptionRepository.save(subscription);

        log.info("Subscription created: {} for tenant: {}", subscription.getId(), request.tenantId());
        auditService.log(AuditAction.CREATE, "Subscription", subscription.getId(), "Created new subscription");

        return mapper.toResponse(subscription);
    }

    @Override
    @Transactional
    @CacheEvict(value = "subscriptions", key = "#tenantId")
    public void cancelSubscription(UUID tenantId) {
        log.info("Cancelling subscription for tenant: {}", tenantId);

        Subscription subscription = subscriptionRepository.findByTenantIdAndIsActiveTrue(tenantId)
                .orElseThrow(() -> new RuntimeException("No active subscription found"));

        subscription.setIsActive(false);
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);

        log.info("Subscription cancelled for tenant: {}", tenantId);
    }
}