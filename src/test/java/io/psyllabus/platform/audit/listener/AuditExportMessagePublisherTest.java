package io.psyllabus.platform.audit.listener;

import io.psyllabus.platform.audit.event.AuditExportQueuedEvent;
import io.psyllabus.platform.common.config.RabbitMQConstants;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditExportMessagePublisherTest {

    @Test
    void onAuditExportQueued_publishesLeanPayload() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        AuditExportMessagePublisher publisher = new AuditExportMessagePublisher(rabbitTemplate);
        UUID jobId = UUID.randomUUID();

        publisher.onAuditExportQueued(new AuditExportQueuedEvent(jobId));

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConstants.AUDIT_EXPORT_EXCHANGE,
                RabbitMQConstants.AUDIT_EXPORT_ROUTING_KEY,
                new AuditExportJobPayload(jobId)
        );
    }
}
