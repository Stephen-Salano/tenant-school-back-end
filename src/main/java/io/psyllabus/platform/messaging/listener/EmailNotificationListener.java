package io.psyllabus.platform.messaging.listener;

import io.psyllabus.platform.messaging.dto.EmailMessage;
import io.psyllabus.platform.messaging.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static io.psyllabus.platform.common.config.RabbitMQConstants.EMAIL_QUEUE;

/**
 * Consumes {@link EmailMessage} payloads from the email notification queue
 * and delegates to {@link EmailService} for rendering and delivery.
 *
 * <p>Retry and DLQ routing are handled by the container factory.
 * Throughput is controlled by the single consumer pattern.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationListener {

    private final EmailService emailService;

    @RabbitListener(
            queues = EMAIL_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void onEmailNotification(EmailMessage message) {
        log.debug("Email message received from queue [to={}, template={}]", 
            message.to(), message.templateType());
        
        emailService.send(message);
    }
}
