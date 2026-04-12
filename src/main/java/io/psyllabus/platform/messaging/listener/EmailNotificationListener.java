package io.psyllabus.platform.messaging.listener;

import io.psyllabus.platform.messaging.dto.EmailMessage;
import io.psyllabus.platform.messaging.entities.ProcessedMessage;
import io.psyllabus.platform.messaging.repository.ProcessedMessageRepository;
import io.psyllabus.platform.messaging.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static io.psyllabus.platform.common.config.RabbitMQConstants.EMAIL_QUEUE;

/**
 * Consumes {@link EmailMessage} payloads from the email notification queue
 * and delegates to {@link EmailService} for rendering and delivery.
 *
 * <p>Implements idempotency via {@link ProcessedMessageRepository} to ensure
 * at-least-once delivery doesn't result in duplicate sends.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationListener {

    private final EmailService emailService;
    private final ProcessedMessageRepository processedMessageRepository;

    @RabbitListener(
            queues = EMAIL_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    @Transactional
    public void onEmailNotification(EmailMessage message) {
        log.debug("Email message received from queue [to={}, template={}]", 
            message.to(), message.templateType());
        
        if (processedMessageRepository.existsById(message.idempotencyKey())) {
            log.warn("Duplicate email skipped: key={}", message.idempotencyKey());
            return;
        }

        emailService.send(message);

        processedMessageRepository.save(ProcessedMessage.builder()
                .idempotencyKey(message.idempotencyKey())
                .tenantId(message.tenantId())
                .messageType("EMAIL")
                .build());
    }
}
