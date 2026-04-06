package io.psyllabus.platform.academic.repository;

import io.psyllabus.platform.academic.entities.AcademicYear;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {
    Page<AcademicYear> findAllByTenantId(UUID tenantId, Pageable pageable);
    Optional<AcademicYear> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<AcademicYear> findByTenantIdAndIsCurrentTrue(UUID tenantId);
    boolean existsByNameAndTenantId(String name, UUID tenantId);
}
