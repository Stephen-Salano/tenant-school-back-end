package io.psyllabus.platform.assignment.repository;

import io.psyllabus.platform.assignment.dto.response.AssignmentDetailResponse;
import io.psyllabus.platform.assignment.dto.response.AssignmentResponse;
import io.psyllabus.platform.assignment.entities.Assignment;
import io.psyllabus.platform.assignment.enums.AssignmentStatus;
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

    @Query("""
        SELECT new io.psyllabus.platform.assignment.dto.response.AssignmentResponse(
            a.id, a.classId, sc.name, a.subjectId, sub.name, 
            a.title, a.description, a.assignmentType, a.maxScore, 
            a.dueDate, a.postedAt, a.status,
            (SELECT COUNT(aa.id) FROM AssignmentAttachment aa WHERE aa.assignmentId = a.id AND aa.tenantId = a.tenantId),
            COUNT(CASE WHEN asub.submissionStatus = 'PENDING' THEN 1 END),
            COUNT(CASE WHEN asub.submissionStatus = 'SUBMITTED' THEN 1 END),
            COUNT(CASE WHEN asub.submissionStatus = 'GRADED' THEN 1 END)
        )
        FROM Assignment a
        JOIN SchoolClass sc ON sc.id = a.classId AND sc.tenantId = a.tenantId
        JOIN Subject sub ON sub.id = a.subjectId AND sub.tenantId = a.tenantId
        LEFT JOIN AssignmentSubmission asub ON asub.assignmentId = a.id AND asub.tenantId = a.tenantId
        WHERE a.classId = :classId AND a.tenantId = :tenantId
        GROUP BY a.id, sc.name, sub.name
    """)
    Page<AssignmentResponse> findResponsesWithStatsByClassId(@Param("classId") UUID classId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("""
        SELECT new io.psyllabus.platform.assignment.dto.response.AssignmentDetailResponse(
            a.id, a.classId, sc.name, a.subjectId, sub.name,
            a.academicTermId, a.staffId, CONCAT(s.firstName, ' ', s.lastName),
            a.title, a.description, a.instructions, a.assignmentType,
            a.maxScore, a.dueDate, a.postedAt, a.closedAt, a.archivedAt, a.status,
            null
        )
        FROM Assignment a
        JOIN SchoolClass sc ON sc.id = a.classId AND sc.tenantId = a.tenantId
        JOIN Subject sub ON sub.id = a.subjectId AND sub.tenantId = a.tenantId
        JOIN Staff s ON s.id = a.staffId AND s.tenantId = a.tenantId
        WHERE a.id = :id AND a.tenantId = :tenantId
    """)
    Optional<AssignmentDetailResponse> findDetailById(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}
