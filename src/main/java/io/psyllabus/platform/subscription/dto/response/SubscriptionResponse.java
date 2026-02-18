package io.psyllabus.platform.subscription.dto.response;

import io.psyllabus.platform.subscription.enums.PlanType;
import io.psyllabus.platform.subscription.enums.SubscriptionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        UUID tenantId,
        PlanType planType,
        LocalDateTime startDate,
        LocalDateTime endDate,
        SubscriptionStatus status,
        List<String> features,
        Boolean isActive
) {
}
