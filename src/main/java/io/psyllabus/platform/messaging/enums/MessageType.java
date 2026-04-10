package io.psyllabus.platform.messaging.enums;

/**
 * Discriminates the nature of a message.
 */
public enum MessageType {
    NOTICE,         // School-wide or group-specific notice
    ANNOUNCEMENT,   // Critical announcement
    SMS_LOG,        // Transactional record of a sent SMS
    EMAIL_LOG       // Transactional record of a sent Email
}
