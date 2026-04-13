package io.psyllabus.platform.academic.repository;

import io.psyllabus.platform.academic.entities.SubjectGroup;
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
public interface SubjectGroupRepository extends JpaRepository<SubjectGroup, UUID> {

    /**
     * Efficiently load all subject group names for a tenant.
     * Used by CsvValidationService to validate staff assignments in bulk.
     */
    @Query("SELECT sg.name FROM SubjectGroup sg WHERE sg.tenantId = :tenantId")
    Set<String> findAllNamesByTenantId(@Param("tenantId") UUID tenantId);

    Page<SubjectGroup> findAllByTenantId(UUID tenantId, Pageable pageable);
    List<SubjectGroup> findAllByIdInAndTenantId(Set<UUID> ids, UUID tenantId);
    Optional<SubjectGroup> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<SubjectGroup> findByNameAndTenantId(String name, UUID tenantId);
    boolean existsByNameAndTenantId(String name, UUID tenantId);

    @Query("SELECT sg.name FROM SubjectGroup sg WHERE sg.id = :id AND sg.tenantId = :tenantId")
    Optional<String> findNameByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}
