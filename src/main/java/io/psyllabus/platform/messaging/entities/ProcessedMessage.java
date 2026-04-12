package io.psyllabus.platform.messaging.entities;

import io.psyllabus.platform.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

/**
 * Tracks RabbitMQ messages that have already been successfully processed.
 * Prevents duplicate actions (e.g. double emails/SMS) in at-least-once delivery.
 */
@Entity
@Table(name = "processed_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedMessage extends Auditable {

    /**
     * The unique idempotency key from the message.
     * Suggested format: type:tenantId:businessKey (e.g. email:UUID:pwd-reset-123)
     */
    @Id
    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /**
     * Type of message processed (EMAIL, SMS, ONBOARDING_IMPORT).
     */
    @Column(name = "message_type", length = 50)
    private String messageType;
}
