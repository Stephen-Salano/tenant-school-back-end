package io.psyllabus.platform.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.psyllabus.platform.common.annotation.RequireFeature;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.psyllabus.platform.common.dto.ErrorResponse;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.subscription.entity.Subscription;
import io.psyllabus.platform.subscription.service.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * SubscriptionInterceptor - Validates subscription and feature access.
 *
 * Execution: After authentication, before controller method
 *
 * Flow:
 * 1. Check if method has @RequireFeature annotation
 * 2. If no annotation → allow access
 * 3. Extract tenant_id from TenantContext
 * 4. Load subscription from SubscriptionService (Cached)
 * 5. Validate subscription is active and not expired
 * 6. Check if required feature is in subscription.features
 * 7. Return true (proceed) or false + 403 (block)
 *
 * @see RequireFeature
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionInterceptor implements HandlerInterceptor {

    private final SubscriptionService subscriptionService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // Only intercept controller methods (no static resources)
        if (!(handler instanceof HandlerMethod handlerMethod)){
            return true;
        }

        // Check if method requires feature
        RequireFeature annotation = handlerMethod.getMethodAnnotation(RequireFeature.class);

        if (annotation == null){
            // No feature requirement == allow access
            return true;
        }

        String requiredFeature = annotation.value();
        log.debug("Checking feature access: {}", requiredFeature);

        // Get tenant ID from context
        UUID tenantId = TenantContext.getCurrentTenantId();

        if (tenantId == null){
            log.error("Tenant context not set in SubscriptionInterceptor");
            writeErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Tenant context missing", "INTERNAL_ERROR");
            return false;
        }

        // Load subscription (cached via service)
        Subscription subscription = subscriptionService.getActiveSubscription(tenantId);

        if (subscription == null){
            log.warn("No active subscription found for tenant: {}", tenantId);
            writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "No active subscription found", "SUBSCRIPTION_NOT_FOUND");
            return false;
        }

        // Validate subscription is active and not expired
        if (!subscription.isValid()){
            log.warn("Subscription expired for tenant: {}", tenantId);
            writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Subscription expired", "SUBSCRIPTION_EXPIRED");
            return false;
        }

        // Check feature access
        if (!subscription.hasFeature(requiredFeature)){
            log.warn("Feature '{}' not available in plan: {} for tenant: {}",
                    requiredFeature, subscription.getPlanType(), tenantId);
            
            ErrorResponse errorDetails = ErrorResponse.builder()
                    .message("Feature not available in current plan")
                    .errorCode("FEATURE_NOT_AVAILABLE")
                    .validationErrors(Map.of(
                            "required_feature", requiredFeature,
                            "current_plan", subscription.getPlanType().name(),
                            "upgrade_required", "true"
                    ))
                    .build();
            
            writeApiResponse(response, HttpServletResponse.SC_FORBIDDEN, ApiResponse.error("Feature not available", errorDetails));
            return false;
        }

        log.debug("Feature access granted: {} for tenant: {}", requiredFeature, tenantId);
        return true;
    }

    private void writeErrorResponse(HttpServletResponse response, int status, String message, String errorCode) throws IOException {
        ErrorResponse errorDetails = ErrorResponse.of(message, errorCode);
        ApiResponse<Void> apiResponse = ApiResponse.error(message, errorDetails);
        writeApiResponse(response, status, apiResponse);
    }

    private void writeApiResponse(HttpServletResponse response, int status, ApiResponse<?> apiResponse) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), apiResponse);
    }
}
