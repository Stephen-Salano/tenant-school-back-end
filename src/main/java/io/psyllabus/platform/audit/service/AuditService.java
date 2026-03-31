package io.psyllabus.platform.audit.service;

import io.psyllabus.platform.audit.enums.AuditAction;
import java.util.UUID;

/**
 * Service for recording security-sensitive operations.
 */
public interface AuditService {

    /**
     * Record an operation in the audit log.
     *
     * @param action the action being performed
     * @param entityType the type of entity affected (e.g., "BRANCH")
     * @param entityId the ID of the affected entity
     * @param details human-readable explanation of the event
     */
    void log(AuditAction action, String entityType, UUID entityId, String details);
}
