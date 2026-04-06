package io.psyllabus.platform.academic.repository;

import io.psyllabus.platform.academic.entities.SubjectGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubjectGroupRepository extends JpaRepository<SubjectGroup, UUID> {
    Page<SubjectGroup> findAllByTenantId(UUID tenantId, Pageable pageable);
    Optional<SubjectGroup> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByNameAndTenantId(String name, UUID tenantId);
}
