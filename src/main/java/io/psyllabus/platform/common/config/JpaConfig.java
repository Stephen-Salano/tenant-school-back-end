package io.psyllabus.platform.common.config;

import io.psyllabus.platform.auth.entities.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA Configuration - Enables Auditing and provides the Auditor provider.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return new AuditorAwareImpl();
    }

    /**
     * Bridges Spring Security with JPA Auditing.
     * Fetches the current User's ID from the SecurityContext.
     */
    public static class AuditorAwareImpl implements AuditorAware<UUID> {
        
        @Override
        @NonNull
        public Optional<UUID> getCurrentAuditor() {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User)) {
                return Optional.empty(); // No authenticated user (e.g., initial onboarding)
            }
            
            return Optional.of(((User) auth.getPrincipal()).getId());
        }
    }
}
