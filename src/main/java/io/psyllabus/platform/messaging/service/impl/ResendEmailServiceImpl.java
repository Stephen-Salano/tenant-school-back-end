package io.psyllabus.platform.messaging.service.impl;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.psyllabus.platform.messaging.enums.EmailType;
import io.psyllabus.platform.messaging.service.EmailService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implementation of EmailService using Resend.
 * Decoupled from SDK initialization via EmailConfig.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResendEmailServiceImpl implements EmailService {

    private final Resend resendClient; // Injected from EmailConfig
    private final MeterRegistry meterRegistry;

    @Value("${resend.from}")
    private String fromAddress;

    @Value("${resend.support-email}")
    private String supportEmail;

    private Counter sentCounter;
    private Counter failedCounter;

    @PostConstruct
    void initMetrics() {
        sentCounter = meterRegistry.counter("notifications.email.sent.total");
        failedCounter = meterRegistry.counter("notifications.email.failed.total");
    }

    @Override
    public void send(String to, String subject, String htmlBody, EmailType type) {
        try {
            var options = CreateEmailOptions.builder()
                    .from(fromAddress)
                    .to(to)
                    .subject(subject)
                    .html(htmlBody)
                    .build();

            var response = resendClient.emails().send(options);
            sentCounter.increment();
            log.info("Email sent [to={}, type={}, messageId={}]", to, type, response.getId());
        } catch (Exception ex) {
            failedCounter.increment();
            log.error("Email delivery failed [to={}, type={}, error={}]", to, type, ex.getMessage());
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
}
