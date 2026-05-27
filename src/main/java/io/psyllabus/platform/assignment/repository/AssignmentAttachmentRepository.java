package io.psyllabus.platform.assignment.repository;

import io.psyllabus.platform.assignment.entities.AssignmentAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssignmentAttachmentRepository extends JpaRepository<AssignmentAttachment, String> {

    List<AssignmentAttachment> findAllByAssignmentIdAndTenantId(UUID assignmentId, UUID tenantId);

    void deleteAllByAssignmentIdAndTenantId(UUID assignmentId, UUID tenantId);

    long countByAssignmentIdAndTenantId(UUID assignmentId, UUID tenantId);

    @Query("""
        SELECT a.assignmentId, COUNT(a)
        FROM AssignmentAttachment a
        WHERE a.assignmentId IN :assignmentIds AND a.tenantId = :tenantId
        GROUP BY a.assignmentId
    """)
    List<Object[]> countByAssignmentIds(@Param("assignmentIds") List<UUID> assignmentIds, @Param("tenantId") UUID tenantId);
}
