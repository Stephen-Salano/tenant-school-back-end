package io.psyllabus.platform.audit.repository;

import io.psyllabus.platform.audit.entitiy.AuditExportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditExportJobRepository extends JpaRepository<AuditExportJob, UUID> {

    Optional<AuditExportJob> findByIdAndRequestingUserId(UUID id, UUID requestingUserId);
}
