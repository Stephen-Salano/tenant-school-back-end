package io.psyllabus.platform.messaging.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Categorizes the purpose of outgoing emails.
 */
@Getter
@RequiredArgsConstructor
public enum EmailType {
    PLATFORM_INQUIRY("Support: Platform Inquiry"),
    PLATFORM_BUG_REPORT("Support: Bug Report"),
    PLATFORM_BILLING("System: Billing & Subscription"),
    USER_WELCOME("Internal: User Welcome"),
    USER_PASSWORD_RESET("Internal: Password Reset"),
    ACADEMIC_PROGRESS_REPORT("Academic: Progress Report"),
    ACADEMIC_ATTENDANCE_ALERT("Academic: Attendance Alert"),
    SCHOOL_ANNOUNCEMENT("School: General Announcement"),
    SCHOOL_EVENT_INVITATION("School: Event Invitation");

    private final String description;
}
