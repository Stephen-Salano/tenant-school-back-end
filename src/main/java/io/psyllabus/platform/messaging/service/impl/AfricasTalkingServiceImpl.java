package io.psyllabus.platform.messaging.service.impl;

import com.africastalking.SmsService;
import com.africastalking.sms.Recipient;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.psyllabus.platform.messaging.enums.SmsType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of SmsService using Africa's Talking.
 * Decoupled from SDK initialization via SmsConfig.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AfricasTalkingServiceImpl implements io.psyllabus.platform.messaging.service.SmsService {

    private final SmsService atSms; // Injected from SmsConfig
    private final MeterRegistry meterRegistry;

    @Value("${africas-talking.sender-id}")
    private String senderId;

    private Counter sentCounter;
    private Counter failedCounter;

    @PostConstruct
    void initMetrics() {
        sentCounter = meterRegistry.counter("notifications.sms.sent.total");
        failedCounter = meterRegistry.counter("notifications.sms.failed.total");
    }

    @Override
    public void send(String message, SmsType type, List<String> recipients) {
        String[] numbers = recipients.toArray(new String[0]);
        try {
            List<Recipient> results = atSms.send(message, senderId, numbers, true);
            for (Recipient r : results) {
                if ("Success".equalsIgnoreCase(r.status)) {
                    sentCounter.increment();
                    log.info("SMS delivered [to={}, type={}, messageId={}]", r.number, type, r.messageId);
                } else {
                    failedCounter.increment();
                    log.warn("SMS rejected [to={}, type={}, status={}]", r.number, type, r.status);
                }
            }
        } catch (Exception ex) {
            failedCounter.increment();
            log.error("SMS dispatch error [type={}, error={}]", type, ex.getMessage());
        }
    }
}
