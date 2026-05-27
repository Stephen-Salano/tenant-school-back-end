package io.psyllabus.platform.subscription.controller;

import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.psyllabus.platform.subscription.dto.request.SubscriptionCreateRequest;
import io.psyllabus.platform.subscription.dto.response.SubscriptionResponse;
import io.psyllabus.platform.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Subscriptions", description = "Subscription management for school tenants")
public class SubscriptionController extends BaseController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/current")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get active subscription for current tenant")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getCurrent() {
        UUID tenantId = getCurrentTenantId();
        log.info("Get subscription request: tenant={}", tenantId);
        var subscription = subscriptionService.getActiveSubscription(tenantId);
        if (subscription == null) {
            return ResponseEntity.ok(ApiResponse.success(null, "No active subscription found"));
        }
        return ok(new SubscriptionResponse(
                subscription.getId(),
                subscription.getTenantId(),
                subscription.getPlanType(),
                subscription.getStartDate(),
                subscription.getEndDate(),
                subscription.getStatus(),
                subscription.getFeatures(),
                subscription.getIsActive()
        ), "Active subscription retrieved");
    }

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Create subscription for a tenant")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> create(
            @Valid @RequestBody SubscriptionCreateRequest request) {
        log.info("Create subscription request: tenant={}, plan={}", request.tenantId(), request.planType());
        return created(subscriptionService.createSubscription(request), "Subscription created");
    }

    @DeleteMapping("/current")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel active subscription for current tenant")
    public ResponseEntity<ApiResponse<Void>> cancel() {
        UUID tenantId = getCurrentTenantId();
        log.info("Cancel subscription request: tenant={}", tenantId);
        subscriptionService.cancelSubscription(tenantId);
        return okMessage("Subscription cancelled");
    }
}