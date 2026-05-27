package io.psyllabus.platform.grading.repository;

import io.psyllabus.platform.grading.entities.GradingLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GradingLevelRepository extends JpaRepository<GradingLevel, UUID> {

    List<GradingLevel> findAllBySchemeIdAndTenantIdOrderBySortOrderAsc(UUID schemeId, UUID tenantId);

    void deleteAllBySchemeIdAndTenantId(UUID schemeId, UUID tenantId);

    @Query("""
        SELECT l.schemeId, COUNT(l)
        FROM GradingLevel l
        WHERE l.schemeId IN :schemeIds AND l.tenantId = :tenantId
        GROUP BY l.schemeId
    """)
    List<Object[]> countBySchemeIds(@Param("schemeIds") List<UUID> schemeIds, @Param("tenantId") UUID tenantId);
}
