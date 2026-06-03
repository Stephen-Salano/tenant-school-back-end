package io.psyllabus.platform.tenant.secret.repository;

import io.psyllabus.platform.tenant.secret.entity.TenantChannelConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TenantChannelConfigRepository extends JpaRepository<TenantChannelConfig, UUID> {
}
