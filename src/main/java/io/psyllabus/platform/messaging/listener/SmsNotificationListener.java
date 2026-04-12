package io.psyllabus.platform.messaging.listener;

import io.psyllabus.platform.common.constants.FeatureConstants;
import io.psyllabus.platform.messaging.dto.SmsMessage;
import io.psyllabus.platform.messaging.entities.ProcessedMessage;
import io.psyllabus.platform.messaging.repository.ProcessedMessageRepository;
import io.psyllabus.platform.messaging.service.SmsService;
import io.psyllabus.platform.subscription.entity.Subscription;
import io.psyllabus.platform.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static io.psyllabus.platform.common.config.RabbitMQConstants.SMS_QUEUE;

/**
 * Consumes {@link SmsMessage} payloads and delegates to {@link SmsService}.
 * 
 * <p>Implements idempotency via {@link ProcessedMessageRepository} and
 * enforces subscription gates for SMS features.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SmsNotificationListener {

    private final SmsService smsService;
    private final SubscriptionService subscriptionService;
    private final ProcessedMessageRepository processedMessageRepository;

    @RabbitListener(
            queues = SMS_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    @Transactional
    public void onSmsNotification(SmsMessage message) {
        UUID tenantId = message.tenantId();

        if (processedMessageRepository.existsById(message.idempotencyKey())) {
            log.warn("Duplicate SMS skipped: key={}", message.idempotencyKey());
            return;
        }
        
        // 1. Fetch active subscription for the tenant
        Subscription subscription = subscriptionService.getActiveSubscription(tenantId);
        
        // 2. Validate subscription existence and status
        if (subscription == null || !subscription.isValid()) {
            log.warn("SMS skipped — No active/valid subscription found [tenantId={}, type={}]", 
                tenantId, message.type());
            return;
        }

        // 3. Check if the specific SMS feature is enabled in their plan
        if (!subscription.hasFeature(FeatureConstants.SMS_NOTIFICATIONS)) {
            log.debug("SMS skipped — Feature '{}' not in plan [tenantId={}, plan={}]", 
                FeatureConstants.SMS_NOTIFICATIONS, tenantId, subscription.getPlanType());
            return; 
        }

        log.debug("SMS message received [type={}, tenantId={}]", message.type(), tenantId);
        smsService.send(message);

        processedMessageRepository.save(ProcessedMessage.builder()
                .idempotencyKey(message.idempotencyKey())
                .tenantId(tenantId)
                .messageType("SMS")
                .build());
    }
}
