package io.psyllabus.platform.tenant.secret.repository;

import io.psyllabus.platform.tenant.secret.entity.TenantSecret;
import io.psyllabus.platform.tenant.secret.enums.SecretType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantSecretRepository extends JpaRepository<TenantSecret, UUID> {

    Optional<TenantSecret> findByTenantIdAndSecretType(UUID tenantId, SecretType secretType);

    List<TenantSecret> findAllByTenantId(UUID tenantId);

    boolean existsByTenantIdAndSecretType(UUID tenantId, SecretType secretType);

    void deleteByTenantIdAndSecretType(UUID tenantId, SecretType secretType);
}
