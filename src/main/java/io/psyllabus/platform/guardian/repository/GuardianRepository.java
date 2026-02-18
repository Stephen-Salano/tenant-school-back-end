package io.psyllabus.platform.guardian.repository;

import io.psyllabus.platform.guardian.entities.Guardian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GuardianRepository extends JpaRepository<Guardian, UUID> {

    Optional<Guardian> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Guardian> findByEmailAndTenantId(String email, UUID tenantId);

    boolean existsByEmailAndTenantId(String email, UUID tenantId);

    boolean existsByPhoneAndTenantId(String phone, UUID tenantId);

    List<Guardian> findByTenantId(UUID tenantId);
}
