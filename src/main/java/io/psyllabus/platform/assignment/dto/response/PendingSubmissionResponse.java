package io.psyllabus.platform.assignment.dto.response;

import io.psyllabus.platform.assignment.enums.SubmissionType;

import java.time.LocalDateTime;
import java.util.UUID;

public record PendingSubmissionResponse(
        UUID submissionId,
        UUID assignmentId,
        String assignmentTitle,
        UUID studentId,
        String studentName,
        String admissionNumber,
        String className,
        String subjectName,
        LocalDateTime submittedAt,
        SubmissionType submissionType
) {}
