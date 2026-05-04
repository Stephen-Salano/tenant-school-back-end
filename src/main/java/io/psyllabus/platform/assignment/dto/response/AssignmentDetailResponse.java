package io.psyllabus.platform.assignment.dto.response;

import io.psyllabus.platform.assignment.enums.AssignmentStatus;
import io.psyllabus.platform.assignment.enums.AssignmentType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AssignmentDetailResponse(
        UUID id,
        UUID classId,
        String className,
        UUID subjectId,
        String subjectName,
        UUID academicTermId,
        UUID staffId,
        String teacherName,
        String title,
        String description,
        String instructions,
        AssignmentType assignmentType,
        Integer maxScore,
        LocalDateTime dueDate,
        LocalDateTime postedAt,
        LocalDateTime closedAt,
        LocalDateTime archivedAt,
        AssignmentStatus status,
        List<AttachmentMetadataResponse> attachments
) {
}
