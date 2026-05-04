package io.psyllabus.platform.academic.dto.response;

import io.psyllabus.platform.academic.enums.AssignmentStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for {@link io.psyllabus.platform.academic.entities.TeacherAssignment}.
 */
public record TeacherAssignmentResponse(
    UUID id,
    UUID staffId,
    UUID userId,
    String staffName,
    UUID classId,
    String className,
    UUID subjectId,
    String subjectName,
    UUID academicTermId,
    String academicTermName,
    boolean isClassTeacher,
    Integer lessonsPerWeek,
    AssignmentStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
