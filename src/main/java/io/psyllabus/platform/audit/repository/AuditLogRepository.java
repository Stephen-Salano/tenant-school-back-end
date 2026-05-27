package io.psyllabus.platform.audit.repository;

import io.psyllabus.platform.audit.entitiy.AuditLog;
import io.psyllabus.platform.audit.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findAllByTenantId(UUID tenantId, Pageable pageable);

    Page<AuditLog> findAllByTenantIdAndUserId(UUID tenantId, UUID userId, Pageable pageable);

    Page<AuditLog> findAllByTenantIdAndAction(UUID tenantId, AuditAction action, Pageable pageable);

    Page<AuditLog> findAllByTenantIdAndEntityType(UUID tenantId, String entityType, Pageable pageable);

    Page<AuditLog> findAllByTenantIdAndCreatedAtBetween(UUID tenantId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:entityType IS NULL OR a.entityType = :entityType) " +
           "AND (:userId IS NULL OR a.userId = :userId) " +
           "AND (:fromDate IS NULL OR a.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR a.createdAt <= :toDate)")
    Page<AuditLog> searchByFilters(
            @Param("tenantId") UUID tenantId,
            @Param("action") AuditAction action,
            @Param("entityType") String entityType,
            @Param("userId") UUID userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    List<AuditLog> findAllByTenantIdAndCreatedAtBetween(UUID tenantId, LocalDateTime from, LocalDateTime to);

    List<AuditLog> findAllByTenantId(UUID tenantId);
}