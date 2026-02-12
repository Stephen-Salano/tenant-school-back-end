package io.psyllabus.platform.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark controller methods that require a specific subscription feature.
 * Used by SubscriptionInterceptor to enforce plan limits.
 *
 * Example:
 * @RequireFeature("teacher_ai")
 * public ResponseEntity<Insight> getAiInsights() { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireFeature {
    /**
     * The name of the feature required to access this endpoint.
     * e.g., "teacher_ai", "advanced_analytics"
     */
    String value();
}
