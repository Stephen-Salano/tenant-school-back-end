package io.psyllabus.platform.academic.repository;

import io.psyllabus.platform.academic.entities.SubjectCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubjectCategoryRepository extends JpaRepository<SubjectCategory, UUID> {
    Page<SubjectCategory> findAllByTenantId(UUID tenantId, Pageable pageable);
    java.util.List<SubjectCategory> findAllByTenantId(UUID tenantId);
    Optional<SubjectCategory> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByNameAndTenantId(String name, UUID tenantId);
}
