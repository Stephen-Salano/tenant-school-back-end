package io.psyllabus.platform.common.config;

import com.resend.Resend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Factory for Email Infrastructure.
 * Centralizes the initialization of the Resend client.
 */
@Configuration
public class EmailConfig {

    @Value("${resend.api-key}")
    private String apiKey;

    @Bean
    public Resend resendClient() {
        return new Resend(apiKey);
    }
}
