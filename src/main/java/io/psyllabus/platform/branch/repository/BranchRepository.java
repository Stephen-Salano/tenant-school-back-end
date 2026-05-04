package io.psyllabus.platform.branch.repository;

import io.psyllabus.platform.branch.entities.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface BranchRepository extends JpaRepository<Branch, UUID> {

    /**
     * Efficiently load all branch codes for a tenant.
     * Used by CsvValidationService to validate branch assignments in bulk.
     */
    @Query("SELECT b.code FROM Branch b WHERE b.tenantId = :tenantId")
    Set<String> findAllCodesByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Efficient existence check for cross-tenant isolation.
     */
    boolean existsByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Finds a branch by ID, ensuring it belongs to the specified tenant.
     */
    Optional<Branch> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Branch> findByCodeAndTenantId(String code, UUID tenantId);

    /**
     * Lists all branches belonging to a specific school (tenant).
     */
    List<Branch> findAllByTenantId(UUID tenantId);

    /**
     * Counts how many branches a school currently has.
     * Used to enforce subscription plan limits.
     */
    long countByTenantId(UUID tenantId);

    /**
     * Checks if a branch code (e.g., "NORTH") is already taken within a school.
     */
    boolean existsByCodeAndTenantId(String code, UUID tenantId);

    /**
     * Checks if a branch name (e.g., "Main Campus") is already taken within a school.
     */
    boolean existsByNameAndTenantId(String name, UUID tenantId);
}
