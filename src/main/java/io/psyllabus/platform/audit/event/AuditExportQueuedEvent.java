package io.psyllabus.platform.audit.event;

import java.util.UUID;

public record AuditExportQueuedEvent(UUID jobId) {
}
