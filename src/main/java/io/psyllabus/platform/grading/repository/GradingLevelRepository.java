package io.psyllabus.platform.grading.repository;

import io.psyllabus.platform.grading.entities.GradingLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GradingLevelRepository extends JpaRepository<GradingLevel, UUID> {

    List<GradingLevel> findAllBySchemeIdAndTenantIdOrderBySortOrderAsc(UUID schemeId, UUID tenantId);

    void deleteAllBySchemeIdAndTenantId(UUID schemeId, UUID tenantId);
}
