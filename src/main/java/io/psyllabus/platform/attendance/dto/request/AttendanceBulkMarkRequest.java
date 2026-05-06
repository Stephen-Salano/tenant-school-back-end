package io.psyllabus.platform.attendance.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AttendanceBulkMarkRequest(
        @NotNull(message = "Class ID is required")
        UUID classId,

        @NotNull(message = "Attendance date is required")
        LocalDate date,

        @NotBlank(message = "Idempotency key is required")
        String idempotencyKey,

        @NotEmpty(message = "Attendance records are required")
        List<@Valid AttendanceMarkRequest> records
) {
}
