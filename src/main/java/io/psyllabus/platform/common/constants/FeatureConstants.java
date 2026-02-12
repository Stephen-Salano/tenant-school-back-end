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

    // AI Features
    public static final String TEACHER_AI = "teacher_ai";
    public static final String PARENT_AI = "parent_ai";
    public static final String INSTITUTIONAL_AI = "institutional_ai";

    // Analytics Features
    public static final String ADVANCED_ANALYTICS = "advanced_analytics";
    public static final String PREDICTIVE_ANALYTICS = "predictive_analytics";
    public static final String CUSTOM_REPORTS = "custom_reports";

    // Infrastructure Features
    public static final String MULTI_CAMPUS = "multi_campus";
    public static final String CUSTOM_DOMAINS = "custom_domains";
//    public static final String API_ACCESS = "api_access";

    // Communication Features
    public static final String SMS_NOTIFICATIONS = "sms_notifications";
    public static final String BULK_EMAIL = "bulk_email";
//    public static final String PARENT_PORTAL_CUSTOMIZATION = "parent_portal_customization";

    // Storage & Capacity
    public static final String UNLIMITED_STORAGE = "unlimited_storage";
    public static final String VIDEO_HOSTING = "video_hosting";

    // Support Features
//    public static final String PRIORITY_SUPPORT = "priority_support";
//    public static final String DEDICATED_ACCOUNT_MANAGER = "dedicated_account_manager";

    private FeatureConstants(){
        throw new UnsupportedOperationException("Utility class - cannot instantiate");
    }
}
