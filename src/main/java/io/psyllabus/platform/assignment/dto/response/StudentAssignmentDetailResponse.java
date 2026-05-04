package io.psyllabus.platform.assignment.dto.response;

import io.psyllabus.platform.assignment.enums.AssignmentType;
import io.psyllabus.platform.assignment.enums.SubmissionStatus;
import io.psyllabus.platform.assignment.enums.SubmissionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record StudentAssignmentDetailResponse(
        UUID submissionId,
        UUID assignmentId,
        String title,
        String subjectName,
        String className,
        AssignmentType assignmentType,
        LocalDateTime dueDate,
        LocalDateTime postedAt,
        boolean overdue,
        Integer maxScore,
        String gradingSchemeName,
        String description,
        String instructions,
        String teacherName,
        List<AttachmentMetadataResponse> teacherAttachments,
        SubmissionStatus submissionStatus,
        LocalDateTime submittedAt,
        SubmissionType submissionType,
        String textContent,
        List<AttachmentMetadataResponse> submittedFiles,
        Integer score,
        String gradeLabel,
        String feedback,
        LocalDateTime gradedAt,
        boolean late
) {
}
