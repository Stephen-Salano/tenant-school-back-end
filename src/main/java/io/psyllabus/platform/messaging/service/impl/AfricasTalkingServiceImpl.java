package io.psyllabus.platform.messaging.service.impl;

import com.africastalking.SmsService;
import com.africastalking.sms.Recipient;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.psyllabus.platform.common.exception.MessagingException;
import io.psyllabus.platform.messaging.dto.SmsMessage;
import io.psyllabus.platform.messaging.enums.SmsType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Implementation of SmsService using Africa's Talking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AfricasTalkingServiceImpl implements io.psyllabus.platform.messaging.service.SmsService {

    private final SmsService atSms; 
    private final MeterRegistry meterRegistry;

    @Value("${africas-talking.sender-id}")
    private String senderId;

    private Counter sentCounter;
    private Counter failedCounter;
    private Timer sendTimer;

    private static final Map<SmsType, String> TEMPLATES = Map.of(
        SmsType.FEE_REMINDER, "Reminder: {studentName} has an outstanding fee of KES {amount}. Pay at {loginUrl}.",
        SmsType.EXAM_RESULTS, "Exam results for {studentName} are available. Log in to {schoolName} to view.",
        SmsType.OTP_VERIFICATION, "Your Psyllabus security code is {otp}.",
        SmsType.BROADCAST, "{message}",
        SmsType.EMERGENCY_NOTICE, "EMERGENCY: {message}"
    );

    @PostConstruct
    void initMetrics() {
        sentCounter = meterRegistry.counter("notifications.sms.sent.total");
        failedCounter = meterRegistry.counter("notifications.sms.failed.total");
        sendTimer = meterRegistry.timer("notifications.sms.duration.seconds");
    }

    @Override
    public void send(SmsMessage message) {
        String maskedTo = maskPhone(message.to());
        log.info("SMS send start [to={}, type={}]", maskedTo, message.type());
        sendTimer.record(() -> doSend(message, maskedTo));
    }

    private void doSend(SmsMessage message, String maskedTo) {
        try {
            String body = renderTemplate(message.type(), message.variables());
            List<Recipient> results = atSms.send(body, senderId, new String[]{message.to()}, true);
            for (Recipient r : results) {
                if ("Success".equalsIgnoreCase(r.status)) {
                    sentCounter.increment();
                    log.info("SMS delivered [to={}, type={}, messageId={}]", maskedTo, message.type(), r.messageId);
                } else {
                    failedCounter.increment();
                    log.warn("SMS delivery issue [to={}, type={}, status={}]", maskedTo, message.type(), r.status);
                }
            }
        } catch (Exception ex) {
            failedCounter.increment();
            log.error("SMS dispatch error [to={}, type={}, error={}]", maskedTo, message.type(), ex.getMessage());
            throw new MessagingException("Failed to dispatch SMS through Africa's Talking", ex);
        }
    }

    private String renderTemplate(SmsType type, Map<String, String> vars) {
        String template = TEMPLATES.getOrDefault(type, "{message}");
        if (vars == null || vars.isEmpty()) return template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            template = template.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return template;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }
}
