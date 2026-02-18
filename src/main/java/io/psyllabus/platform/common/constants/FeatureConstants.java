package io.psyllabus.platform.common.constants;

/**
 * Feature codes for subscription-based access control.
 * These constants match the feature codes stored in subscriptions.features JSON column.
 *
 * Usage:
 * <pre>
 * {@code
 * @RequireFeature(FeatureConstants.TEACHER_AI)
 * public ResponseEntity<AIInsights> analyze() { ... }
 * }
 * </pre>
 */
public final class FeatureConstants {

    // Analytics Features
    public static final String ADVANCED_ANALYTICS = "advanced_analytics"; // PRO+: Time-based, comparative
    public static final String PREDICTIVE_ANALYTICS = "predictive_analytics"; // ENTERPRISE: Longitudinal
    public static final String CUSTOM_REPORTS = "custom_reports"; // ENTERPRISE: Board-level, regulator-ready
    public static final String CUSTOM_DASHBOARDS = "custom_dashboards"; // ENTERPRISE: Custom KPIs

    // AI Features
    public static final String TEACHER_INTERVENTION = "teacher_intervention"; // PRO+: AI-driven diagnostics
    public static final String STUDENT_GUIDANCE = "student_guidance"; // PRO+: AI-driven motivation
    public static final String INSTITUTIONAL_AI = "institutional_ai"; // ENTERPRISE: Systemic intelligence

    // Infrastructure Features
    public static final String MULTI_CAMPUS = "multi_campus"; // ENTERPRISE: Multi-campus analytics
    public static final String CUSTOM_DOMAINS = "custom_domains"; // ENTERPRISE: Vanity URLs
    public static final String SCHOOL_BRANDING = "school_branding"; // PRO+: Colors, logo

    // Communication Features
    // TODO: Set SMS limits for different tiers
    public static final String SMS_NOTIFICATIONS = "sms_notifications"; // PRO+: SMS alerts
    public static final String BULK_SMS = "bulk_sms"; // ENTERPRISE: Higher SMS limits
    public static final String EMAIL_NOTIFICATIONS = "email_notifications";

    // Integration & Export
    public static final String ADVANCED_EXPORTS = "advanced_exports"; // ENTERPRISE: System integrations

    // Support Features
    public static final String PRIORITY_SUPPORT = "priority_support"; // PRO+: 24hr email support
    public static final String DEDICATED_SUPPORT = "dedicated_support"; // ENTERPRISE: Account manager


    private FeatureConstants(){
        throw new UnsupportedOperationException("Utility class - cannot instantiate");
    }
}
