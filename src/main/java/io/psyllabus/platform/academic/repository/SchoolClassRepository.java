package io.psyllabus.platform.academic.repository;

import io.psyllabus.platform.academic.entities.SchoolClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, UUID> {
    Page<SchoolClass> findAllByTenantId(UUID tenantId, Pageable pageable);
    Page<SchoolClass> findAllByAcademicYearIdAndTenantId(UUID academicYearId, UUID tenantId, Pageable pageable);
    Page<SchoolClass> findAllByBranchIdAndTenantId(UUID branchId, UUID tenantId, Pageable pageable);
    Optional<SchoolClass> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByNameAndAcademicYearIdAndTenantId(String name, UUID academicYearId, UUID tenantId);
}
