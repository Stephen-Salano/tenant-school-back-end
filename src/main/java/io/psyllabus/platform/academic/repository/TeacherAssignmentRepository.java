package io.psyllabus.platform.academic.repository;
import io.psyllabus.platform.academic.dto.response.TeacherAssignmentResponse;
import io.psyllabus.platform.academic.entities.TeacherAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, UUID> {
    Page<TeacherAssignment> findAllByTenantId(UUID tenantId, Pageable pageable);
    List<TeacherAssignment> findAllByStaffIdAndTenantId(UUID staffId, UUID tenantId);
    List<TeacherAssignment> findAllByClassIdAndTenantId(UUID classId, UUID tenantId);

    @Query("""
        SELECT new io.psyllabus.platform.academic.dto.response.TeacherAssignmentResponse(
            ta.id, ta.staffId, s.userId, CONCAT(s.firstName, ' ', s.lastName),
            ta.classId, sc.name, sc.academicYearId, ay.name,
            (SELECT COUNT(sce.id) FROM StudentClassEnrollment sce WHERE sce.classId = sc.id AND sce.tenantId = ta.tenantId AND sce.isActive = true),
            ta.subjectId, sub.name, 
            ta.academicTermId, term.name, ta.isClassTeacher, 
            ta.lessonsPerWeek, ta.status, ta.createdAt, ta.updatedAt
        )
        FROM TeacherAssignment ta
        JOIN Staff s ON s.id = ta.staffId AND s.tenantId = ta.tenantId
        JOIN SchoolClass sc ON sc.id = ta.classId AND sc.tenantId = ta.tenantId
        JOIN AcademicYear ay ON ay.id = sc.academicYearId AND ay.tenantId = sc.tenantId
        JOIN Subject sub ON sub.id = ta.subjectId AND sub.tenantId = ta.tenantId
        JOIN AcademicTerm term ON term.id = ta.academicTermId AND term.tenantId = ta.tenantId
        WHERE ta.classId = :classId AND ta.tenantId = :tenantId
    """)
    List<TeacherAssignmentResponse> findResponsesByClassId(@Param("classId") UUID classId, @Param("tenantId") UUID tenantId);

    @Query("""
        SELECT new io.psyllabus.platform.academic.dto.response.TeacherAssignmentResponse(
            ta.id, ta.staffId, s.userId, CONCAT(s.firstName, ' ', s.lastName),
            ta.classId, sc.name, sc.academicYearId, ay.name,
            (SELECT COUNT(sce.id) FROM StudentClassEnrollment sce WHERE sce.classId = sc.id AND sce.tenantId = ta.tenantId AND sce.isActive = true),
            ta.subjectId, sub.name, 
            ta.academicTermId, term.name, ta.isClassTeacher, 
            ta.lessonsPerWeek, ta.status, ta.createdAt, ta.updatedAt
        )
        FROM TeacherAssignment ta
        JOIN Staff s ON s.id = ta.staffId AND s.tenantId = ta.tenantId
        JOIN SchoolClass sc ON sc.id = ta.classId AND sc.tenantId = ta.tenantId
        JOIN AcademicYear ay ON ay.id = sc.academicYearId AND ay.tenantId = sc.tenantId
        JOIN Subject sub ON sub.id = ta.subjectId AND sub.tenantId = ta.tenantId
        JOIN AcademicTerm term ON term.id = ta.academicTermId AND term.tenantId = ta.tenantId
        WHERE ta.staffId = :staffId AND ta.tenantId = :tenantId
    """)
    List<TeacherAssignmentResponse> findResponsesByStaffId(@Param("staffId") UUID staffId, @Param("tenantId") UUID tenantId);

    List<TeacherAssignment> findAllByAcademicTermIdAndTenantId(UUID academicTermId, UUID tenantId);

    @Query("SELECT ta FROM TeacherAssignment ta WHERE ta.classId = :classId AND ta.tenantId = :tenantId AND ta.isClassTeacher = true AND ta.status = 'ACTIVE'")
    Optional<TeacherAssignment> findClassTeacherByClassIdAndTenantId(@Param("classId") UUID classId, @Param("tenantId") UUID tenantId);

    Optional<TeacherAssignment> findByIdAndTenantId(UUID id, UUID tenantId);
    List<TeacherAssignment> findAllByClassIdAndAcademicTermIdAndIsClassTeacherTrueAndTenantId(UUID classId, UUID academicTermId, UUID tenantId);
    boolean existsByStaffIdAndClassIdAndSubjectIdAndAcademicTermIdAndTenantIdAndStatus(
            UUID staffId,
            UUID classId,
            UUID subjectId,
            UUID academicTermId,
            UUID tenantId,
            io.psyllabus.platform.academic.enums.AssignmentStatus status
    );

    boolean existsByStaffIdAndClassIdAndSubjectIdAndAcademicTermIdAndTenantId(
        UUID staffId, UUID classId, UUID subjectId, UUID academicTermId, UUID tenantId);

    boolean existsByStaffIdAndClassIdAndTenantIdAndStatus(
            UUID staffId,
            UUID classId,
            UUID tenantId,
            io.psyllabus.platform.academic.enums.AssignmentStatus status
    );
}
