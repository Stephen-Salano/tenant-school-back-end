package io.psyllabus.platform.subscription.dto.request;

import io.psyllabus.platform.subscription.enums.PlanType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SubscriptionCreateRequest(
        @NotNull(message = "Tenant ID is required")
        UUID tenantId,

        @NotNull(message = "Plan type is required")
        PlanType planType,

        LocalDateTime startDate,

        LocalDateTime endDate,

        List<String> features
) {
}
