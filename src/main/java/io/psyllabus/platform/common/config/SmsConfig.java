package io.psyllabus.platform.common.config;

import com.africastalking.AfricasTalking;
import com.africastalking.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Factory for SMS Infrastructure.
 * Centralizes Africa's Talking SDK initialization.
 * 
 * <p>Condition: Requires {@code africas-talking.api-key} to be set.
 * If missing, the SmsService bean is not created and SMS functionality is disabled.
 */
@Configuration
@Slf4j
public class SmsConfig {

    @Value("${africas-talking.username:#{null}}")
    private String username;

    @Value("${africas-talking.api-key:#{null}}")
    private String apiKey;

    @Bean
    @ConditionalOnProperty(name = "africas-talking.api-key")
    public SmsService africasTalkingSms() {
        if (username == null || apiKey == null) {
            log.warn("Africa's Talking credentials missing — SMS functionality will be disabled.");
            return null;
        }
        AfricasTalking.initialize(username, apiKey);
        log.info("Africa's Talking SMS service initialized [username={}]", username);
        return AfricasTalking.getService(AfricasTalking.SERVICE_SMS);
    }
}
