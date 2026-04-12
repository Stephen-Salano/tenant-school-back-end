package io.psyllabus.platform.common.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cloudinary configuration for image and document storage.
 * 
 * <p>Condition: Requires {@code cloudinary.api-key} to be set.
 * If missing, the Cloudinary bean is not created and storage functionality is limited.
 */
@Configuration
@Slf4j
public class StorageConfig {

    @Value("${cloudinary.cloud-name:#{null}}")
    private String cloudName;

    @Value("${cloudinary.api-key:#{null}}")
    private String apiKey;

    @Value("${cloudinary.api-secret:#{null}}")
    private String apiSecret;

    @Bean
    @ConditionalOnProperty(name = "cloudinary.api-key")
    public Cloudinary cloudinary() {
        if (cloudName == null || apiKey == null || apiSecret == null) {
            log.warn("Cloudinary credentials missing — storage functionality will be disabled.");
            return null;
        }
        log.info("Cloudinary storage initialized [cloudName={}]", cloudName);
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }
}
