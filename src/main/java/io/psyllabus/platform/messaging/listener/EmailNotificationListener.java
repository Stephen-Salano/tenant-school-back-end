package io.psyllabus.platform.messaging.listener;

import com.resend.Resend;
import io.psyllabus.platform.messaging.dto.EmailMessage;
import io.psyllabus.platform.messaging.entities.ProcessedMessage;
import io.psyllabus.platform.messaging.enums.EmailTemplateType;
import io.psyllabus.platform.messaging.repository.ProcessedMessageRepository;
import io.psyllabus.platform.messaging.service.EmailService;
import io.psyllabus.platform.tenant.secret.service.TenantChannelConfigService;
import io.psyllabus.platform.tenant.secret.service.TenantResendFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static io.psyllabus.platform.common.config.RabbitMQConstants.EMAIL_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationListener {

    private final EmailService emailService;
    private final ProcessedMessageRepository processedMessageRepository;
    private final TenantChannelConfigService channelConfigService;
    private final TenantResendFactory tenantResendFactory;

    private static final Set<EmailTemplateType> PLATFORM_TEMPLATES = Set.of(
            EmailTemplateType.WELCOME_ADMIN,
            EmailTemplateType.WELCOME_ADMIN_CONFIRMATION
    );

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

        UUID tenantId = message.tenantId();

        Resend client;
        if (PLATFORM_TEMPLATES.contains(message.templateType())) {
            client = tenantResendFactory.getPlatformClient();
            if (client == null) {
                log.warn("Platform Resend client not available — skipping admin email [template={}]", message.templateType());
                return;
            }
            log.debug("Using platform Resend client for template={}", message.templateType());
        } else {
            if (!channelConfigService.isEmailEnabled(tenantId)) {
                log.debug("Email disabled for tenant={} — skipping [template={}]", tenantId, message.templateType());
                return;
            }

            Optional<Resend> tenantClient = tenantResendFactory.getClient(tenantId);
            if (tenantClient.isEmpty()) {
                log.warn("No Resend key configured for tenant={} — skipping [template={}]", tenantId, message.templateType());
                return;
            }
            client = tenantClient.get();
            log.debug("Using tenant Resend client for tenant={}, template={}", tenantId, message.templateType());
        }

        emailService.send(message, client);

        processedMessageRepository.save(ProcessedMessage.builder()
                .idempotencyKey(message.idempotencyKey())
                .tenantId(tenantId)
                .messageType("EMAIL")
                .build());
    }
}
