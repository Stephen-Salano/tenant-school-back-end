package io.psyllabus.platform.academic.enums;

/**
 * Lifecycle status for a {@link io.psyllabus.platform.academic.entities.TeacherAssignment}.
 * ACTIVE — the assignment is current and visible in timetables.
 * INACTIVE — soft-deleted; kept for historical reporting.
 */
public enum AssignmentStatus {
    ACTIVE,
    INACTIVE
}
