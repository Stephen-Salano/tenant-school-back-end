package io.psyllabus.platform.platformadmin.entities;

import io.psyllabus.platform.auth.enums.UserStatus;
import io.psyllabus.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Platform-level administrator identity.
 *
 * <p>Deliberately has <strong>no {@code tenant_id}</strong> column. Platform admins
 * operate across all tenants and are stored in a separate table to preserve the
 * {@code NOT NULL} constraint on {@code users.tenant_id}. This prevents any
 * school-level user (student, parent, staff) from being created without a tenant,
 * which would be an orphaned record and a data-integrity failure.</p>
 *
 * <p>Spring Security: authorities return a single {@code ROLE_PLATFORM_ADMIN} grant.
 * {@code TenantFilter} and {@code JwtAuthFilter} detect this role and apply the
 * PLATFORM_ADMIN bypass path (issue #2).</p>
 *
 * <p>K-DPA: no student or school data is associated with this entity. Audit trail
 * is inherited from {@link Auditable}. The {@code created_by} column will carry
 * {@code AuditingConfig.SYSTEM_UUID} for the first seeded admin because no
 * platform principal is in {@code SecurityContextHolder} at seed time. This is
 * acceptable and can be tightened when a platform-admin auditor provider is added.</p>
 */

@Table(
        name = "platform_admins",
        indexes = {
                @Index(name = "idx_platform_admin_username", columnList = "username", unique = true),
                @Index(name = "idx_platform_admin_email", columnList = "email", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformAdmin extends Auditable implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "hashed_password", nullable = false, length = 255)
    private String hashedPassword;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    // ── UserDetails ────────────────────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));
    }

    @Override
    public String getPassword() {
        return hashedPassword;
    }

    @Override
    public boolean isAccountNonExpired() {
        return status != UserStatus.INACTIVE;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.SUSPENDED;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
