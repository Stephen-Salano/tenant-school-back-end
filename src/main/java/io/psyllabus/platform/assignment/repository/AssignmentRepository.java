package io.psyllabus.platform.assignment.repository;

import io.psyllabus.platform.assignment.entities.Assignment;
import io.psyllabus.platform.assignment.enums.AssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    Optional<Assignment> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<Assignment> findAllByStaffIdAndTenantId(UUID staffId, UUID tenantId, Pageable pageable);

    Page<Assignment> findAllByStaffIdAndClassIdAndTenantId(UUID staffId, UUID classId, UUID tenantId, Pageable pageable);

    Page<Assignment> findAllByStaffIdAndTenantIdAndStatus(UUID staffId, UUID tenantId, AssignmentStatus status, Pageable pageable);

    Page<Assignment> findAllByStaffIdAndClassIdAndTenantIdAndStatus(
            UUID staffId,
            UUID classId,
            UUID tenantId,
            AssignmentStatus status,
            Pageable pageable
    );

    List<Assignment> findAllByIdInAndTenantId(List<UUID> ids, UUID tenantId);
}
