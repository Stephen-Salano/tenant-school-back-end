package io.psyllabus.platform.assignment.event;

import io.psyllabus.platform.assignment.enums.SubmissionType;

import java.util.UUID;

public record SubmissionReceivedEvent(
        String idempotencyKey,
        UUID submissionId,
        UUID assignmentId,
        UUID studentId,
        UUID staffId,
        UUID tenantId,
        SubmissionType submissionType
) {
}
