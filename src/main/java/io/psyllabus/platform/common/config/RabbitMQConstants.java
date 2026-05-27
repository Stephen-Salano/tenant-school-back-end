package io.psyllabus.platform.common.config;

/** Central reference for all RabbitMQ exchange, queue, and routing-key names. */
public final class RabbitMQConstants {
    private RabbitMQConstants() {}

    // ── Notification exchange (Topic) ─────────────────────────────────────
    public static final String NOTIFICATIONS_EXCHANGE = "psyllabus.notifications";
    public static final String NOTIFICATIONS_DLX = "psyllabus.notifications.dlx";
    
    public static final String EMAIL_QUEUE = "psyllabus.notifications.email";
    public static final String EMAIL_DLQ = "psyllabus.notifications.email.dlq";
    public static final String EMAIL_ROUTING_KEY = "notification.email.#"; // topic wildcard

    public static final String SMS_QUEUE = "psyllabus.notifications.sms";
    public static final String SMS_DLQ = "psyllabus.notifications.sms.dlq";
    public static final String SMS_ROUTING_KEY = "notification.sms.#"; // topic wildcard

    // ── Onboarding exchange (Direct) ──────────────────────────────────────
    public static final String ONBOARDING_EXCHANGE = "psyllabus.onboarding";
    public static final String ONBOARDING_DLX = "psyllabus.onboarding.dlx";
    
    public static final String STUDENT_IMPORT_QUEUE = "psyllabus.onboarding.student-import";
    public static final String STUDENT_IMPORT_DLQ = "psyllabus.onboarding.student-import.dlq";
    public static final String STUDENT_IMPORT_ROUTING_KEY = "onboarding.student";

    public static final String STAFF_IMPORT_QUEUE = "psyllabus.onboarding.staff-import";
    public static final String STAFF_IMPORT_DLQ = "psyllabus.onboarding.staff-import.dlq";
    public static final String STAFF_IMPORT_ROUTING_KEY = "onboarding.staff";

    // ── Audit export exchange (Direct) ─────────────────────────────────────
    public static final String AUDIT_EXPORT_EXCHANGE = "psyllabus.audit-export";
    public static final String AUDIT_EXPORT_DLX = "psyllabus.audit-export.dlx";

    public static final String AUDIT_EXPORT_QUEUE = "psyllabus.audit-export.jobs";
    public static final String AUDIT_EXPORT_DLQ = "psyllabus.audit-export.jobs.dlq";
    public static final String AUDIT_EXPORT_ROUTING_KEY = "audit.export.job";

    // ── Assignments exchange (Direct) ─────────────────────────────────────
    public static final String ASSIGNMENT_EXCHANGE = "psyllabus.assignments";
    public static final String ASSIGNMENT_DLX = "psyllabus.assignments.dlx";

    public static final String ASSIGNMENT_PUBLISHED_QUEUE = "psyllabus.assignments.published";
    public static final String ASSIGNMENT_PUBLISHED_DLQ = "psyllabus.assignments.published.dlq";
    public static final String ASSIGNMENT_PUBLISHED_KEY = "assignment.published";

    public static final String SUBMISSION_RECEIVED_QUEUE = "psyllabus.assignments.submission.received";
    public static final String SUBMISSION_RECEIVED_DLQ = "psyllabus.assignments.submission.received.dlq";
    public static final String SUBMISSION_RECEIVED_KEY = "assignment.submission.received";

    public static final String SUBMISSION_GRADED_QUEUE = "psyllabus.assignments.submission.graded";
    public static final String SUBMISSION_GRADED_DLQ = "psyllabus.assignments.submission.graded.dlq";
    public static final String SUBMISSION_GRADED_KEY = "assignment.submission.graded";
}
