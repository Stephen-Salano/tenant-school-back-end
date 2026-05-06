package io.psyllabus.platform.attendance.dto.request;

import io.psyllabus.platform.attendance.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AttendanceMarkRequest(
        @NotNull(message = "Student ID is required")
        UUID studentId,

        @NotNull(message = "Attendance status is required")
        AttendanceStatus status
) {
}
