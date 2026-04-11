package io.psyllabus.platform.messaging.enums;

/**
 * Maps each notification event to a Thymeleaf template file.
 * Template files live in {@code resources/templates/emails/}.
 * 
 * <p>As per MEMORY.md, activation emails for SM4 are manual only.
 */
public enum EmailTemplateType {
    PASSWORD_RESET ("password-reset"),
    IMPORT_COMPLETE_SUMMARY ("import-complete-summary"), // sent to admin
    IMPORT_FAILED ("import-failed"), // sent to admin
    SYSTEM_NOTIFICATION ("system-notification"); // Generic 1:1 internal

    /** Matches the .html filename under templates/emails/. */
    public final String templateName;

    EmailTemplateType(String templateName) {
        this.templateName = templateName;
    }
}
