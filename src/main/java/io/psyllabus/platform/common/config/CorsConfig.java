package io.psyllabus.platform.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${spring.application.frontend-url}")
    private String frontendUrl;

    private final Environment environment;

    public CorsConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Main API CORS configuration
        CorsConfiguration configuration = new CorsConfiguration();

        // Determine allowed origins based on environment
        List<String> allowedOrigins = getAllowedOrigins();
        configuration.setAllowedOriginPatterns(allowedOrigins);

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "X-Requested-With",
                "Cache-Control",
                "X-Tenant-ID",       // ← add: dev TenantFilter bypass header
                "X-Platform-Key"
        ));

        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        // Apply to API endpoints only
        source.registerCorsConfiguration("/api/**", configuration);

        // H2 Console - Allow all origins without CORS restrictions (only active in dev/test via SecurityConfig)
        CorsConfiguration h2Config = new CorsConfiguration();
        h2Config.addAllowedOriginPattern("*");
        h2Config.addAllowedHeader("*");
        h2Config.addAllowedMethod("*");
        h2Config.setAllowCredentials(false);
        source.registerCorsConfiguration("/h2-console/**", h2Config);

        return source;
    }

    private List<String> getAllowedOrigins() {
        String activeProfile = Arrays.stream(environment.getActiveProfiles())
                .findFirst()
                .orElse("dev");

        return switch (activeProfile) {
            case "prod" -> List.of(frontendUrl, "https://*.psyllabus.io"); // Replace with actual domain
            case "staging" -> List.of(frontendUrl, "https://*.staging.psyllabus.io"); // Replace with actual staging domain
            default -> List.of(
                    "http://localhost:3000",
                    "http://localhost:5173",
                    "http://localhost:4200",
                    "http://127.0.0.1:3000",
                    "http://127.0.0.1:5173"
            );
        };
    }
}
