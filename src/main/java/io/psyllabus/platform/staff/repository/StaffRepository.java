package io.psyllabus.platform.staff.repository;

import io.psyllabus.platform.staff.entities.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface StaffRepository extends JpaRepository<Staff, UUID> {

    /**
     * Efficiently load all employee numbers for a tenant.
     * Used by CsvValidationService to check for duplicates in bulk.
     */
    @Query("SELECT s.employeeNumber FROM Staff s WHERE s.tenantId = :tenantId")
    Set<String> findAllEmployeeNumbersByTenantId(@Param("tenantId") UUID tenantId);

    Optional<Staff> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Staff> findByEmployeeNumberAndTenantId(String employeeNumber, UUID tenantId);

    boolean existsByEmployeeNumberAndTenantId(String employeeNumber, UUID tenantId);

    boolean existsByEmailAndTenantId(String email, UUID tenantId);

    // ── Added for Stage 5 ──────────────────────────────────────────────────────
    Page<Staff> findByTenantId(UUID tenantId, Pageable pageable);

    long countByTenantId(UUID tenantId);
}
