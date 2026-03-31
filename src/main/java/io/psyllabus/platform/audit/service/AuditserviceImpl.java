package io.psyllabus.platform.audit.service;

import io.psyllabus.platform.audit.entitiy.AuditLog;
import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.repository.AuditLogRepository;
import io.psyllabus.platform.common.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of AuditService.
 * Uses MANDATORY propagation to ensure audit is part of the parent transaction,
 * or REQUIRES_NEW if you want the audit to persist even if the main task fails.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditserviceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void log(AuditAction action, String entityType, UUID entityId, String details) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        
        // Safety check — some system-level events might not have a tenant context
        if (tenantId == null) {
            log.warn("Attempted to log audit event without tenant context: {} on {}", action, entityType);
            return;
        }

        AuditLog auditLog = AuditLog.builder()
                .tenantId(tenantId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit logged: {} on {} (ID: {})", action, entityType, entityId);
    }
}
