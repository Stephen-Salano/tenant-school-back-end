package io.psyllabus.platform.messaging.dto;

import io.psyllabus.platform.auth.enums.UserType;
import io.psyllabus.platform.messaging.enums.EmailTemplateType;
import java.util.Map;
import java.util.UUID;

/**
 * Payload published to {@code psyllabus.notifications.email} queue.
 *
 * <p>K-DPA note: {@code variables} must never contain a plaintext password.
 * Pass {@code loginUrl} and {@code admissionNumber} only — the temp password
 * is communicated by the school admin out-of-band.
 *
 * @param idempotencyKey unique key to prevent duplicate sends
 * @param tenantId school/tenant identifier for plan/quota checks
 * @param to recipient email address
 * @param toName recipient display name (used in template greeting)
 * @param recipientType STUDENT | STAFF | PARENT
 * @param templateType which Thymeleaf template to render
 * @param variables template variables injected into the HTML body
 */
public record EmailMessage(
    String idempotencyKey,
    UUID tenantId,
    String to,
    String toName,
    UserType recipientType,
    EmailTemplateType templateType,
    Map<String, String> variables
) {}
