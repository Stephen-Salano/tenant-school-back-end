package io.psyllabus.platform.common.exception;

/**
 * Custom exception for messaging failures (SMS, Email, In-App).
 */
public class MessagingException extends RuntimeException {
    public MessagingException(String message) {
        super(message);
    }

    public MessagingException(String message, Throwable cause) {
        super(message, cause);
    }
}
