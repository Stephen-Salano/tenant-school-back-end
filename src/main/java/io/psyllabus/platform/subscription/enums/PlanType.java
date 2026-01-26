package io.psyllabus.platform.subscription.enums;

/**
 * Subscription plan types
 * Determines which features are available to a school
 */
public enum PlanType {
    /**
     * Basic Plan - Core academic management only.
     * Features: Students, staff, classes, grades, attendance.
     * No AI features, basic analytics only.
     */
    BASIC,
    /**
     * Professional Plan - Includes AI assistants.
     * All Basic features plus:
     * - Teacher AI assistant (diagnostic support)
     * - Parent AI assistant (motivational insights)
     * - SMS notifications (1,500/month)
     * - Advanced analytics
     */
    PROFESSIONAL,
    /**
     * Enterprise Plan - Full feature access.
     * All Professional features plus:
     * - Institutional AI (trend analysis, strategic planning)
     * - Career guidance AI module
     * - Multi-campus analytics
     * - Custom reports
     * - Dedicated account manager
     * - Custom domain support
     */
    ENTERPRISE
}
