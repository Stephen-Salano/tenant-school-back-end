package io.psyllabus.platform.academic.repository;

import io.psyllabus.platform.academic.entities.TeacherAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, UUID> {
    Page<TeacherAssignment> findAllByTenantId(UUID tenantId, Pageable pageable);
    List<TeacherAssignment> findAllByStaffIdAndTenantId(UUID staffId, UUID tenantId);
    List<TeacherAssignment> findAllByClassIdAndTenantId(UUID classId, UUID tenantId);
    List<TeacherAssignment> findAllByAcademicTermIdAndTenantId(UUID academicTermId, UUID tenantId);
    Optional<TeacherAssignment> findByIdAndTenantId(UUID id, UUID tenantId);
    List<TeacherAssignment> findAllByClassIdAndAcademicTermIdAndIsClassTeacherTrueAndTenantId(UUID classId, UUID academicTermId, UUID tenantId);

    boolean existsByStaffIdAndClassIdAndSubjectIdAndAcademicTermIdAndTenantId(
        UUID staffId, UUID classId, UUID subjectId, UUID academicTermId, UUID tenantId);
}
