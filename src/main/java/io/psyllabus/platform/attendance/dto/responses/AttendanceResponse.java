package io.psyllabus.platform.attendance.dto.responses;

import io.psyllabus.platform.attendance.enums.AttendanceStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record AttendanceResponse(
        UUID recordId,
        UUID studentId,
        String studentName,
        String admissionNumber,
        UUID classId,
        String className,
        UUID academicTermId,
        LocalDate date,
        AttendanceStatus status,
        UUID markedBy,
        LocalDateTime markedAt
) {
}
