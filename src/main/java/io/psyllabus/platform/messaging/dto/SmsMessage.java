package io.psyllabus.platform.messaging.dto;

import io.psyllabus.platform.messaging.enums.SmsType;
import java.util.Map;
import java.util.UUID;

/**
 * Payload published to {@code psyllabus.notifications.sms} queue.
 *
 * <p>K-DPA: {@code variables} must never contain passwords or sensitive health data.
 *
 * @param idempotencyKey unique key to prevent duplicate sends
 * @param to E.164 phone number e.g. {@code +254712345678}
 * @param tenantId used by the listener to check the school's subscription plan
 * @param type which message template to render
 * @param variables placeholders injected into the template string
 */
public record SmsMessage(
    String idempotencyKey,
    String to,
    UUID tenantId,
    SmsType type,
    Map<String, String> variables
) {}
