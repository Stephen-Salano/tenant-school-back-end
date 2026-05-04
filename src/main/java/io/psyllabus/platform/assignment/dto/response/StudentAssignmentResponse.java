package io.psyllabus.platform.assignment.dto.response;

import io.psyllabus.platform.assignment.enums.SubmissionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record StudentAssignmentResponse(
        UUID submissionId,
        UUID assignmentId,
        String title,
        String subjectName,
        String description,
        LocalDateTime dueDate,
        boolean overdue,
        LocalDateTime submittedAt,
        SubmissionStatus submissionStatus,
        String teacherName,
        long attachmentCount,
        Integer score,
        Integer maxScore,
        String gradeLabel,
        String feedback
) {
}
