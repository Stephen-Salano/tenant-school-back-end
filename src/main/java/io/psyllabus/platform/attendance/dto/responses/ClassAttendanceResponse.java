package io.psyllabus.platform.attendance.dto.responses;

/**
 * Whole class attendance for a date
 * { classId, date, students: [{ schoolId, name, status }, ...], stats }
 */
public record ClassAttendanceResponse() {
}
