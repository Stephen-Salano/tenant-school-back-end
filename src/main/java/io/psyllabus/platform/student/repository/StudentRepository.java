package io.psyllabus.platform.student.repository;

import io.psyllabus.platform.student.entities.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    Optional<Student> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Student> findByAdmissionNumberAndTenantId(String admissionNumber, UUID tenantId);

    boolean existsByAdmissionNumberAndTenantId(String admissionNumber, UUID tenantId);

    Page<Student> findByTenantId(UUID tenantId, Pageable pageable);

    long countByTenantId(UUID tenantId);

    /**
     * Count active students enrolled in a specific class.
     * Used by ClassServiceImpl for enrollment stats and slot calculations.
     *
     * @param currentClassId the class UUID
     * @param tenantId the tenant UUID (multi-tenancy isolation)
     * @return number of students whose currentClassId matches
     */
    long countByCurrentClassIdAndTenantId(UUID currentClassId, UUID tenantId);
}
