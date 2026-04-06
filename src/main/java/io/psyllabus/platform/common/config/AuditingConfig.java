package io.psyllabus.platform.common.config;

import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.platformadmin.entities.PlatformAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
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
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AuditingConfig {

    // Stable sentinel for system/unauthenticated writes - not a real user
    public static final UUID SYSTEM_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Bean
    public AuditorAware<UUID> auditorAware(){
        return () -> {
            // Check if explicitly marked as a system process (annotation or context)
            if (io.psyllabus.platform.common.util.SystemContext.isSystemProcess()) {
                return Optional.of(SYSTEM_UUID);
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated()) {
                return Optional.of(SYSTEM_UUID);
            }

            Object principal = auth.getPrincipal();

            if (principal instanceof PlatformAdmin admin){
                return Optional.of(admin.getId());
            }
            if (principal instanceof User user){
                return Optional.of(user.getId());
            }
            return Optional.of(SYSTEM_UUID);
        };
    }
}
