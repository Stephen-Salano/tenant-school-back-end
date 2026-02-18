package io.psyllabus.platform.guardian.repository;

import io.psyllabus.platform.guardian.entities.StudentGuardian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentGuardianRepository extends JpaRepository<StudentGuardian, UUID> {
    List<StudentGuardian> findByStudentIdAndTenantId(UUID studentId, UUID tenantId);

    List<StudentGuardian> findByGuardianIdAndTenantId(UUID guardianId, UUID tenantId);

    Optional<StudentGuardian> findByStudentIdAndGuardianIdAndTenantId(
            UUID studentId, UUID guardianId, UUID tenantId
    );

    boolean existsByStudentIdAndGuardianIdAndTenantId(
            UUID studentId, UUID guardianId, UUID tenantId
    );

    void deleteByStudentIdAndGuardianIdAndTenantId(
            UUID studentId, UUID guardianId, UUID tenantId
    );
}
