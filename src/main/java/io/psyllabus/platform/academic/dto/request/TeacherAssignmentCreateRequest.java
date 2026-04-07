package io.psyllabus.platform.academic.dto.request;

import io.psyllabus.platform.academic.enums.AssignmentStatus;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO for creating a new {@link io.psyllabus.platform.academic.entities.TeacherAssignment}.
 */
public record TeacherAssignmentCreateRequest(
    @NotNull(message = "Staff ID is required")
    UUID staffId,

    @NotNull(message = "Class ID is required")
    UUID classId,

    @NotNull(message = "Subject ID is required")
    UUID subjectId,

    @NotNull(message = "Academic term ID is required")
    UUID academicTermId,

    /**
     * Homeroom / class teacher flag.
     */
    boolean isClassTeacher,

    /** How many lessons per week this teacher delivers for this assignment. */
    Integer lessonsPerWeek,

    /** Status of the assignment. Defaults to ACTIVE. */
    AssignmentStatus status
) {}
