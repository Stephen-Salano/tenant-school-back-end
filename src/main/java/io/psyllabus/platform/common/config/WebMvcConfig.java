package io.psyllabus.platform.common.config;

import io.psyllabus.platform.common.interceptor.SubscriptionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for interceptors and other web layer customizations.
 *
 * Registers:
 * - SubscriptionInterceptor for feature-based access control
 *
 * Execution order:
 * 1. TenantFilter (servlet filter)
 * 2. JwtAuthFilter (servlet filter)
 * 3. SubscriptionInterceptor (Spring interceptor) ← Registered here
 * 4. Controller method execution
 */

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final SubscriptionInterceptor subscriptionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(subscriptionInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",                       // Public - login
                        "/api/auth/register",                    // Public - registration
                        "/api/auth/forgot-password/**",          // Public - password reset
                        "/actuator/**",                          // Public - actuator endpoints
                        "/swagger-ui/**",                        // Public - Swagger UI
                        "/v3/api-docs/**",                       // Public - OpenAPI docs
                        "/error"                                 // Public - Error page
                );
    }
}
