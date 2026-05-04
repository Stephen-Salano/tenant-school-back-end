package io.psyllabus.platform.assignment.dto.response;

import io.psyllabus.platform.assignment.enums.SubmissionStatus;
import io.psyllabus.platform.assignment.enums.SubmissionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SubmissionResponse(
        UUID submissionId,
        UUID assignmentId,
        UUID studentId,
        String studentName,
        String admissionNumber,
        SubmissionStatus submissionStatus,
        SubmissionType submissionType,
        LocalDateTime submittedAt,
        boolean isLate,
        String textContent,
        List<AttachmentMetadataResponse> attachments,
        Integer score,
        Integer maxScore,
        String gradeLabel,
        String feedback,
        LocalDateTime gradedAt
) {
}
