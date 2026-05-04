package io.psyllabus.platform.assignment.event;

import java.util.UUID;

public record AssignmentPublishedEvent(
        String idempotencyKey,
        UUID assignmentId,
        UUID tenantId
) {
}
