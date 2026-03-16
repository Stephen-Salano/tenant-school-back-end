package io.psyllabus.platform.platformadmin.repositories;

import io.psyllabus.platform.platformadmin.entities.PlatformAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link PlatformAdmin}.
 *
 * <p>Intentionally has <strong>no tenant-scoped queries</strong> — platform admins
 * are global. No {@code findByIdAndTenantId} variant exists or should be added.</p>
 */

@Repository
public interface PlatformAdminRepository extends JpaRepository<PlatformAdmin, UUID> {

    /**
     * Primary login lookup — used by {@code JwtAuthFilter} when {@code user_type = PLATFORM_ADMIN}.
     *
     * @param username the platform admin's username
     * @return the matching admin, or empty if not found
     */
    Optional<PlatformAdmin> findByUsername(String username);

    Optional<PlatformAdmin> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
