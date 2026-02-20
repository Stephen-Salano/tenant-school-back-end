package io.psyllabus.platform.common.config;

import io.psyllabus.platform.auth.entities.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Configures JPA auditing — who created/updated a record.
 *
 * Flow:
 *   Authenticated request  → returns current user's UUID
 *   Unauthenticated request (e.g. school onboarding) → returns SYSTEM_UUID
 *
 * SYSTEM_UUID is a stable sentinel — not a real user, just marks
 * system-initiated records (seed data, onboarding, migrations).
 */

@Configuration
public class AuditingConfig {

    // Stable sentinel for system/unathenticated writes - not a real user
    public static final UUID SYSTEM_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Bean
    public AuditorAware<UUID> auditorAware(){
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User user)){
                return Optional.of(SYSTEM_UUID);
            }

            return Optional.of(user.getId());
        };
    }
}
