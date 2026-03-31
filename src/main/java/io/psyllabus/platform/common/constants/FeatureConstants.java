package io.psyllabus.platform.common.constants;

/**
 * Feature flag constants used by SubscriptionInterceptor and featuresFor() in SchoolServiceImpl.
 *
 * Tier mapping (from product docs):
 *   BASIC        → Foundational Stakeholder System (Digital Ops)
 *   PROFESSIONAL → Stakeholder Intelligence & Guided Analytics (AI + Analytics)
 *
 * ── Core Features (ALL plans) ─────────────────────────────────────────────────
 * These are always present. Every school gets them regardless of plan.
 */
public final class FeatureConstants {

    // ── Core Operational Foundation (BASIC + PROFESSIONAL) ───────────────────
    public static final String STUDENT_MANAGEMENT         = "student_management";
    public static final String STAFF_MANAGEMENT           = "staff_management";
    public static final String ATTENDANCE                 = "attendance";
    public static final String GRADES                     = "grades";
    public static final String ANNOUNCEMENTS              = "announcements";
    public static final String ASSIGNMENTS                = "assignments";
    public static final String FEES_TRACKING              = "fees_tracking";
    public static final String EXTRACURRICULAR_TRACKING   = "extracurricular_tracking";
    public static final String EMAIL_NOTIFICATIONS        = "email_notifications";

    // ── Stakeholder Intelligence Layer (PROFESSIONAL ONLY) ───────────────────
    // Administrative Intelligence
    public static final String ADMIN_INTELLIGENCE         = "admin_intelligence";    // Weekly/Monthly trends
    public static final String EARLY_WARNING_SYSTEM       = "early_warning_system";  // Academic decline alerts
    public static final String FINANCIAL_ANALYTICS        = "financial_analytics";   // Income monitoring

    // Teacher Intelligence
    public static final String TEACHER_DIAGNOSTICS        = "teacher_diagnostics";   // Identify struggling/high-performers
    public static final String INTERVENTION_SUPPORT       = "intervention_support";  // Suggested teacher actions

    // Parent & Student Intelligence
    public static final String PROGRESS_TRENDS            = "progress_trends";       // Time-based performance charts
    public static final String MOTIVATIONAL_FEEDBACK      = "motivational_feedback"; // AI feedback & study tips
    public static final String PARENT_GUIDANCE            = "parent_guidance";       // Clear academic explanations

    // Communication & Support
    public static final String SMS_NOTIFICATIONS          = "sms_notifications";     // Pro: 2,000/month
    public static final String SCHOOL_BRANDING            = "school_branding";       // Pro: Colors, Logo
    public static final String PRIORITY_SUPPORT           = "priority_support";      // Pro: 24hr response

    // Infrastructure (Currently available to all in the absence of ENTERPRISE)
    // Multi-campus is allowed for both, but could be restricted if needed later.
    public static final String MULTI_CAMPUS               = "multi_campus";

    private FeatureConstants(){
        throw new UnsupportedOperationException("Utility class - cannot instantiate");
    }
}
