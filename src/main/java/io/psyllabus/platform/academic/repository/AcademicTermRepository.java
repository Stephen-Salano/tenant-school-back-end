package io.psyllabus.platform.academic.repository;

import io.psyllabus.platform.academic.entities.AcademicTerm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AcademicTermRepository extends JpaRepository<AcademicTerm, UUID> {
    Page<AcademicTerm> findAllByTenantId(UUID tenantId, Pageable pageable);
    List<AcademicTerm> findAllByAcademicYearIdAndTenantId(UUID academicYearId, UUID tenantId);
    Optional<AcademicTerm> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<AcademicTerm> findByTenantIdAndIsCurrentTrue(UUID tenantId);
    Optional<AcademicTerm> findByTenantIdAndIsCurrentTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            UUID tenantId,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate
    );
    long countByAcademicYearIdAndTenantId(UUID academicYearId, UUID tenantId);
    boolean existsByAcademicYearIdAndTermNumberAndTenantId(UUID academicYearId, int termNumber, UUID tenantId);

    List<AcademicTerm> findAllByEndDateBefore(java.time.LocalDate endDate);
}
