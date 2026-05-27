package io.psyllabus.platform.assignment.repository;

import io.psyllabus.platform.assignment.entities.SubmissionAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubmissionAttachmentRepository extends JpaRepository<SubmissionAttachment, String> {

    List<SubmissionAttachment> findAllBySubmissionIdAndTenantId(UUID submissionId, UUID tenantId);

    List<SubmissionAttachment> findAllBySubmissionIdInAndTenantId(List<UUID> submissionIds, UUID tenantId);

    void deleteAllBySubmissionIdAndTenantId(UUID submissionId, UUID tenantId);
}
