package io.psyllabus.platform.student.repository;

import io.psyllabus.platform.student.entities.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    Optional<Student> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Student> findByAdmissionNumberAndTenantId(String admissionNumber, UUID tenantId);

    boolean existsByAdmissionNumberAndTenantId(String admissionNumber, UUID tenantId);

}
