package io.psyllabus.platform.messaging.service.impl;

import com.resend.services.emails.model.CreateEmailOptions;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.psyllabus.platform.common.exception.MessagingException;
import io.psyllabus.platform.messaging.dto.EmailMessage;
import io.psyllabus.platform.messaging.service.EmailService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Implementation of EmailService using Resend and Thymeleaf.
 * Per MEMORY.md, this service handles operational and admin notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResendEmailServiceImpl implements EmailService {

    private final com.resend.Resend resendClient;
    private final SpringTemplateEngine emailTemplateEngine;
    private final MeterRegistry meterRegistry;

    @Value("${resend.from:noreply@psyllabus.co.ke}")
    private String fromAddress;

    @Value("${resend.from-name:Psyllabus}")
    private String fromName;

    @Value("${resend.support-email:support@psyllabus.co.ke}")
    private String supportEmail;

    private Counter sentCounter;
    private Counter failedCounter;
    private Timer sendTimer;


    @PostConstruct
    void initMetrics() {
        sentCounter = meterRegistry.counter("notifications.email.sent.total");
        failedCounter = meterRegistry.counter("notifications.email.failed.total");
        sendTimer = meterRegistry.timer("notifications.email.duration.seconds");
    }

    @Override
    public void send(EmailMessage message) {
        log.info("Email send start [to={}, template={}, recipientType={}]", 
            message.to(), message.templateType(), message.recipientType());
        
        sendTimer.record(() -> doSend(message));
    }


    private void doSend(EmailMessage message) {
        try {
            String html = renderTemplate(message);
            var options = CreateEmailOptions.builder()
                    .from(fromName + " <" + fromAddress + ">")
                    .to(message.to())
                    .subject(subjectFor(message))
                    .html(html)
                    .build();

            var response = resendClient.emails().send(options);
            sentCounter.increment();
            log.info("Email delivered [to={}, template={}, messageId={}]", 
                message.to(), message.templateType(), response.getId());
        } catch (Exception ex) {
            failedCounter.increment();
            log.error("Email delivery failed [to={}, template={}, error={}]", 
                message.to(), message.templateType(), ex.getMessage());
            throw new MessagingException("Failed to deliver email through Resend", ex);
        }
    }

    @Override
    public void sendToSupport(String from, String subject, String body) {
        try {
            String htmlBody = String.format("<p><strong>From:</strong> %s</p><p>%s</p>", from, body);
            var options = CreateEmailOptions.builder()
                    .from(fromAddress)
                    .to(supportEmail)
                    .replyTo(from)
                    .subject("Support Request: " + subject)
                    .html(htmlBody)
                    .build();

            resendClient.emails().send(options);
            sentCounter.increment();
            log.info("Support email routed successfully [from={}]", from);
        } catch (Exception ex) {
            failedCounter.increment();
            log.error("Support email routing failed [from={}, error={}]", from, ex.getMessage());
        }
    }

    private String renderTemplate(EmailMessage message) {
        Context ctx = new Context();
        ctx.setVariable("toName", message.toName());
        if (message.variables() != null) {
            message.variables().forEach(ctx::setVariable);
        }
        return emailTemplateEngine.process(message.templateType().templateName, ctx);
    }

    private String subjectFor(EmailMessage message) {
        return switch (message.templateType()) {
            case PASSWORD_RESET -> "Reset your Psyllabus password";
            case IMPORT_COMPLETE_SUMMARY -> "Bulk import completed — Psyllabus";
            case IMPORT_FAILED -> "Bulk import failed — action required";
            case SYSTEM_NOTIFICATION -> "New notification from Psyllabus";
        };
    }
}
