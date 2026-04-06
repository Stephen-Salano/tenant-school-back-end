package io.psyllabus.platform.academic.repository;

import io.psyllabus.platform.academic.entities.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    Page<Subject> findAllByTenantId(UUID tenantId, Pageable pageable);
    Page<Subject> findAllBySubjectGroupIdAndTenantId(UUID subjectGroupId, UUID tenantId, Pageable pageable);
    Page<Subject> findAllBySubjectCategoryIdAndTenantId(UUID subjectCategoryId, UUID tenantId, Pageable pageable);
    Optional<Subject> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByNameAndTenantId(String name, UUID tenantId);
    boolean existsByCodeAndTenantId(String code, UUID tenantId);
}
