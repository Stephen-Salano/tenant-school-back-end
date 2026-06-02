package io.psyllabus.platform.assignment.repository;

import io.psyllabus.platform.assignment.entities.AssignmentSubmission;
import io.psyllabus.platform.assignment.enums.SubmissionStatus;
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
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {

    Optional<AssignmentSubmission> findByAssignmentIdAndStudentIdAndTenantId(UUID assignmentId, UUID studentId, UUID tenantId);

    Optional<AssignmentSubmission> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<AssignmentSubmission> findAllByAssignmentIdAndTenantId(UUID assignmentId, UUID tenantId, Pageable pageable);

    Page<AssignmentSubmission> findAllByAssignmentIdAndTenantIdAndSubmissionStatus(
            UUID assignmentId,
            UUID tenantId,
            SubmissionStatus submissionStatus,
            Pageable pageable
    );

    Page<AssignmentSubmission> findAllByStudentIdAndTenantId(UUID studentId, UUID tenantId, Pageable pageable);

    Page<AssignmentSubmission> findAllByStudentIdAndTenantIdAndSubmissionStatus(
            UUID studentId,
            UUID tenantId,
            SubmissionStatus submissionStatus,
            Pageable pageable
    );

    List<AssignmentSubmission> findAllByAssignmentIdAndTenantId(UUID assignmentId, UUID tenantId);

    Page<AssignmentSubmission> findAllByAssignmentIdInAndTenantIdAndSubmissionStatus(
            List<UUID> assignmentIds,
            UUID tenantId,
            SubmissionStatus submissionStatus,
            Pageable pageable
    );

    @Query("""
        SELECT s.studentId
        FROM AssignmentSubmission s
        WHERE s.assignmentId = :assignmentId
          AND s.tenantId = :tenantId
    """)
    List<UUID> findStudentIdsByAssignmentIdAndTenantId(@Param("assignmentId") UUID assignmentId, @Param("tenantId") UUID tenantId);

    @Query("""
        SELECT s.assignmentId,
               COUNT(CASE WHEN s.submissionStatus = 'PENDING' THEN 1 END),
               COUNT(CASE WHEN s.submissionStatus = 'SUBMITTED' THEN 1 END),
               COUNT(CASE WHEN s.submissionStatus = 'GRADED' THEN 1 END)
        FROM AssignmentSubmission s
        WHERE s.assignmentId IN :assignmentIds AND s.tenantId = :tenantId
        GROUP BY s.assignmentId
    """)
    List<Object[]> countSubmissionsByAssignmentIds(@Param("assignmentIds") List<UUID> assignmentIds, @Param("tenantId") UUID tenantId);
}
