package io.psyllabus.platform.academic.repository;

import io.psyllabus.platform.academic.entities.SchoolClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, UUID> {

    /**
     * Efficiently load all active class keys (Grade::Stream) for a tenant.
     * Used by CsvValidationService to validate student enrollment in bulk.
     */
    @Query("""
        SELECT CONCAT(c.gradeLevel, '::', COALESCE(c.streamName, ''))
        FROM SchoolClass c
        JOIN AcademicYear y ON c.academicYearId = y.id
        WHERE c.tenantId = :tenantId AND y.isCurrent = true
    """)
    Set<String> findAllActiveClassKeysByTenantId(@Param("tenantId") UUID tenantId);

    Page<SchoolClass> findAllByTenantId(UUID tenantId, Pageable pageable);
    Page<SchoolClass> findAllByAcademicYearIdAndTenantId(UUID academicYearId, UUID tenantId, Pageable pageable);
    Page<SchoolClass> findAllByBranchIdAndTenantId(UUID branchId, UUID tenantId, Pageable pageable);
    Optional<SchoolClass> findByIdAndTenantId(UUID id, UUID tenantId);
    List<SchoolClass> findAllByIdInAndTenantId(List<UUID> ids, UUID tenantId);
    Optional<SchoolClass> findByGradeLevelAndStreamNameAndTenantId(String gradeLevel, String streamName, UUID tenantId);
    Optional<SchoolClass> findByGradeLevelAndStreamNameIsNullAndTenantId(String gradeLevel, UUID tenantId);
    boolean existsByNameAndAcademicYearIdAndTenantId(String name, UUID academicYearId, UUID tenantId);
    boolean existsByNameAndAcademicYearIdAndTenantIdAndIdNot(String name, UUID academicYearId, UUID tenantId, UUID id);

    @Query("SELECT c.name FROM SchoolClass c WHERE c.id = :id AND c.tenantId = :tenantId")
    Optional<String> findNameByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}
