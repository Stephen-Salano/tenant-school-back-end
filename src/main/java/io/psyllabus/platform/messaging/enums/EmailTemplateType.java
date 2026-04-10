package io.psyllabus.platform.messaging.enums;

import lombok.RequiredArgsConstructor;

/**
 * Maps each notification event to a Thymeleaf template file.
 * Template files live in {@code resources/templates/emails/}.
 */
@RequiredArgsConstructor
public enum EmailTemplateType {
    STUDENT_ACTIVATION ("student-activation"),
    STAFF_ACTIVATION ("staff-activation"),
    GUARDIAN_ACTIVATION ("guardian-activation"),
    PASSWORD_RESET ("password-reset"),
    IMPORT_COMPLETE_SUMMARY ("import-complete-summary"), // sent to admin
    IMPORT_FAILED ("import-failed"); // sent to admin

    /** Matches the .html filename under templates/emails/. */
    public final String templateName;
}
