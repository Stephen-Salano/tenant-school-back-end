package io.psyllabus.platform.messaging.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Defines the type of SMS communication.
 * 
 * <p>Operational only. Per MEMORY.md, no SMS is sent during bulk onboarding (SM4).
 */
@Getter
@RequiredArgsConstructor
public enum SmsType {
    EXAM_RESULTS("Academic: Exam Results"),
    TERM_OPENING("Academic: Term Opening"),
    TERM_CLOSING("Academic: Term Closing"),
    SCHOOL_EVENT("Engagement: School Event"),
    EMERGENCY_NOTICE("Engagement: Emergency Notice"),
    OTP_VERIFICATION("System: OTP Verification"),
    BROADCAST("System: General Broadcast"),
    FEE_REMINDER("Financial: Fee Reminder"); // Added for SM5/PRO usage

    private final String description;
}
