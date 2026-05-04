package io.psyllabus.platform.academic.repository;

import io.psyllabus.platform.academic.entities.StudentClassEnrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentClassEnrollmentRepository extends JpaRepository<StudentClassEnrollment, UUID> {

    List<StudentClassEnrollment> findAllByClassIdAndTenantIdAndIsActiveTrue(UUID classId, UUID tenantId);

    Optional<StudentClassEnrollment> findByStudentIdAndClassIdAndTenantId(UUID studentId, UUID classId, UUID tenantId);

    boolean existsByStudentIdAndClassIdAndTenantId(UUID studentId, UUID classId, UUID tenantId);

    Page<StudentClassEnrollment> findAllByStudentIdAndTenantIdAndIsActiveTrue(UUID studentId, UUID tenantId, Pageable pageable);

    Page<StudentClassEnrollment> findAllByStudentIdAndTenantId(UUID studentId, UUID tenantId, Pageable pageable);

    long countByClassIdAndTenantIdAndIsActiveTrue(UUID classId, UUID tenantId);
}
