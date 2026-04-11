package io.psyllabus.platform.messaging.enums;

/**
 * Discriminates the nature of a message.
 */
public enum MessageType {
    NOTICE,         // School-wide or group-specific notice
    ANNOUNCEMENT,   // Critical announcement
    CHAT,           // In-app 1:1 message (Staff <-> Guardian) - Added for SM4/SM5
    SMS_LOG,        // Transactional record of a sent SMS
    EMAIL_LOG       // Transactional record of a sent Email
}
