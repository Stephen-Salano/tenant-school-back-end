package io.psyllabus.platform.audit.listener;

import io.psyllabus.platform.audit.event.AuditExportQueuedEvent;
import io.psyllabus.platform.common.config.RabbitMQConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditExportMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuditExportQueued(AuditExportQueuedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.AUDIT_EXPORT_EXCHANGE,
                RabbitMQConstants.AUDIT_EXPORT_ROUTING_KEY,
                new AuditExportJobPayload(event.jobId())
        );
        log.info("Published audit export job to RabbitMQ [jobId={}]", event.jobId());
    }
}
