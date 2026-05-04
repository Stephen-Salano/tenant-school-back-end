package io.psyllabus.platform.student.repository;

import io.psyllabus.platform.student.entities.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    /**
     * Efficiently load all admission numbers for a tenant.
     * Used by CsvValidationService to check for duplicates in bulk.
     */
    @Query("SELECT s.admissionNumber FROM Student s WHERE s.tenantId = :tenantId")
    Set<String> findAllAdmissionNumbersByTenantId(@Param("tenantId") UUID tenantId);

    Optional<Student> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Student> findByUserIdAndTenantId(UUID userId, UUID tenantId);

    List<Student> findAllByIdInAndTenantId(List<UUID> ids, UUID tenantId);
    List<Student> findAllByCurrentClassIdAndTenantId(UUID currentClassId, UUID tenantId);

    @Query("""
        SELECT s
        FROM Student s
        JOIN StudentClassEnrollment e
          ON e.studentId = s.id
         AND e.tenantId = s.tenantId
        WHERE e.classId = :classId
          AND e.tenantId = :tenantId
          AND e.isActive = true
    """)
    Page<Student> findAllEnrolledByClassIdAndTenantId(@Param("classId") UUID classId, @Param("tenantId") UUID tenantId, Pageable pageable);

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
     * @param status the student status (usually ACTIVE)
     * @return number of students whose currentClassId and status matches
     */
    long countByCurrentClassIdAndTenantIdAndCurrentStatus(UUID currentClassId, UUID tenantId, io.psyllabus.platform.student.enums.StudentStatus status);
}
