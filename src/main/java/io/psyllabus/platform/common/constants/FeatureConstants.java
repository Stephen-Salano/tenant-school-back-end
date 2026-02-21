package io.psyllabus.platform.common.constants;

/**
 * Feature flag constants used by SubscriptionInterceptor and featuresFor() in SchoolServiceImpl.
 *
 * Tier mapping (from product docs):
 *   BASIC      → Core academic management only
 *   PROFESSIONAL → Core + Institutional/Teacher/Parent AI modules
 *   ENTERPRISE → Pro + Advanced AI, multi-campus, custom reports, dedicated support
 *
 * ── Core Features (ALL plans) ─────────────────────────────────────────────────
 * These are always present. Every school gets them regardless of plan.
 */

public final class FeatureConstants {

    // Core — included in every plan (BASIC, PROFESSIONAL, ENTERPRISE)
    public static final String STUDENT_MANAGEMENT         = "student_management";
    public static final String STAFF_MANAGEMENT           = "staff_management";
    public static final String ATTENDANCE                 = "attendance";
    public static final String GRADES                     = "grades";
    public static final String ANNOUNCEMENTS              = "announcements";
    public static final String ASSIGNMENTS                = "assignments";
    public static final String FEES_TRACKING              = "fees_tracking";
    public static final String EXTRACURRICULAR_TRACKING   = "extracurricular_tracking";
    public static final String EMAIL_NOTIFICATIONS        = "email_notifications";

    // Analytics — PRO+
    public static final String ADVANCED_ANALYTICS         = "advanced_analytics";    // PRO+: time-based, comparative
    public static final String PREDICTIVE_ANALYTICS       = "predictive_analytics";  // ENTERPRISE: longitudinal
    public static final String CUSTOM_REPORTS             = "custom_reports";        // ENTERPRISE: board/regulator-ready
    public static final String CUSTOM_DASHBOARDS          = "custom_dashboards";     // ENTERPRISE: custom KPIs

    // AI — PRO+ (Teacher AI + Parent AI); ENTERPRISE adds institutional-level
    public static final String TEACHER_INTERVENTION       = "teacher_intervention";  // PRO+: AI diagnostics for teachers
    public static final String STUDENT_GUIDANCE           = "student_guidance";      // PRO+: AI motivation for parents
    public static final String INSTITUTIONAL_AI           = "institutional_ai";      // ENTERPRISE: systemic intelligence

    // Infrastructure — PRO+ / ENTERPRISE
    public static final String SCHOOL_BRANDING            = "school_branding";       // PRO+: logo, colors
    public static final String MULTI_CAMPUS               = "multi_campus";          // ENTERPRISE: multi-campus analytics
    public static final String CUSTOM_DOMAINS             = "custom_domains";        // ENTERPRISE: vanity URLs

    // Communication
    public static final String SMS_NOTIFICATIONS          = "sms_notifications";     // PRO+: SMS alerts
    public static final String BULK_SMS                   = "bulk_sms";              // ENTERPRISE: higher SMS limits

    // Export & Integration
    public static final String ADVANCED_EXPORTS           = "advanced_exports";      // ENTERPRISE: system integrations

    // Support
    public static final String PRIORITY_SUPPORT           = "priority_support";      // PRO+: 24hr email support
    public static final String DEDICATED_SUPPORT          = "dedicated_support";     // ENTERPRISE: account manager

    private FeatureConstants(){
        throw new UnsupportedOperationException("Utility class - cannot instantiate");
    }
}
