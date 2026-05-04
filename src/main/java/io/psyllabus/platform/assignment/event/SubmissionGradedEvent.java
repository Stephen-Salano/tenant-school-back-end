package io.psyllabus.platform.assignment.event;

import java.util.UUID;

public record SubmissionGradedEvent(
        String idempotencyKey,
        UUID submissionId,
        UUID assignmentId,
        UUID studentId,
        UUID tenantId,
        Integer score,
        String gradeLabel,
        String feedback
) {
}
