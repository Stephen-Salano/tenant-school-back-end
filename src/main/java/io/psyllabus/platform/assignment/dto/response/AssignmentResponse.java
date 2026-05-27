package io.psyllabus.platform.assignment.dto.response;

import io.psyllabus.platform.assignment.enums.AssignmentStatus;
import io.psyllabus.platform.assignment.enums.AssignmentType;

import java.time.LocalDateTime;
import java.util.UUID;

public record AssignmentResponse(
        UUID id,
        UUID classId,
        String className,
        UUID subjectId,
        String subjectName,
        String title,
        String description,
        AssignmentType assignmentType,
        Integer maxScore,
        LocalDateTime dueDate,
        LocalDateTime postedAt,
        AssignmentStatus status,
        long attachmentCount,
        long pendingCount,
        long submittedCount,
        long gradedCount
) {
}
