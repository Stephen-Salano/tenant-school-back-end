package io.psyllabus.platform.common.config;

import com.africastalking.AfricasTalking;
import com.africastalking.SmsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Factory for SMS Infrastructure.
 * Centralizes Africa's Talking SDK initialization.
 */
@Configuration
public class SmsConfig {

    @Value("${africas-talking.username}")
    private String username;

    @Value("${africas-talking.api-key}")
    private String apiKey;

    @Bean
    public SmsService africasTalkingSms() {
        AfricasTalking.initialize(username, apiKey);
        return AfricasTalking.getService(AfricasTalking.SERVICE_SMS);
    }
}
