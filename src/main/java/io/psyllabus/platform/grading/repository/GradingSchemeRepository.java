package io.psyllabus.platform.grading.repository;

import io.psyllabus.platform.grading.entities.GradingScheme;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GradingSchemeRepository extends JpaRepository<GradingScheme, UUID> {

    Page<GradingScheme> findAllByTenantId(UUID tenantId, Pageable pageable);

    Optional<GradingScheme> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<GradingScheme> findByTenantIdAndIsDefaultTrue(UUID tenantId);

    Optional<GradingScheme> findByTenantIdAndNameIgnoreCase(UUID tenantId, String name);
}
