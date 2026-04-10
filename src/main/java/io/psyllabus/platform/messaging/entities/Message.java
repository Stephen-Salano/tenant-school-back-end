package io.psyllabus.platform.messaging.entities;

import io.psyllabus.platform.common.audit.Auditable;
import io.psyllabus.platform.messaging.enums.MessageAudience;
import io.psyllabus.platform.messaging.enums.MessageType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Represents a piece of communication within the platform.
 * Can be a public notice on a board or a private log of a sent SMS/Email.
 */
@Entity
@Table(
    name = "messages",
    indexes = {
        @Index(name = "idx_messages_tenant_type", columnList = "tenant_id, type"),
        @Index(name = "idx_messages_audience", columnList = "target_audience"),
        @Index(name = "idx_messages_tenant_created", columnList = "tenant_id, created_at DESC")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MessageType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_audience", length = 30)
    private MessageAudience targetAudience;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** URL to attachment stored in Cloudinary (optional) */
    @Column(name = "attachment_url")
    private String attachmentUrl;

    /** For SMS/Email logs: the recipient identifier (e.g. phone number or email) */
    @Column(name = "recipient_identifier")
    private String recipientIdentifier;

    @Column(name = "is_active")
    private boolean isActive = true;
}
