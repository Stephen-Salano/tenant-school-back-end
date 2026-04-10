package io.psyllabus.platform.messaging.service;

import io.psyllabus.platform.messaging.enums.SmsType;
import java.util.List;

/**
 * Service for sending SMS notifications.
 */
public interface SmsService {
    /**
     * Send an SMS to one or more recipients.
     *
     * @param message    the message body
     * @param type       the type of SMS for categorization
     * @param recipients E.164 formatted numbers (e.g., {@code +254712345678})
     */
    void send(String message, SmsType type, List<String> recipients);
}
