package io.psyllabus.platform.auth.dto.responses;

import io.psyllabus.platform.auth.enums.AccessScope;
import io.psyllabus.platform.auth.enums.UserType;
import io.psyllabus.platform.subscription.enums.PlanType;

import java.util.List;
import java.util.UUID;

public record LoginResponse (
        String token,
        String tokenType,
        long expiresIn, // Milliseconds

        // User info
        UUID userId,
        String username,
        String firstName,
        String lastName,
        UserType userType,

        // Tenant info (from JWT)
        UUID tenantId,
        UUID branchId,
        AccessScope accessScope,

        // Subscription Info
        PlanType subscriptionPlan,
        List<String> features // Enabled features based on subscription
) {
}
