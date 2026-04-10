package io.psyllabus.platform.messaging.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Defines the type of SMS communication.
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
    BROADCAST("System: General Broadcast");

    private final String description;
}
