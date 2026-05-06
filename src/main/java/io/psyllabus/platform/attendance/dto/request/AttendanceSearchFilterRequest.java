package io.psyllabus.platform.attendance.dto.request;

import io.psyllabus.platform.attendance.enums.AttendanceStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AttendanceSearchFilterRequest(
        UUID tenantId,
        String studentName,
        UUID studentId,
        List<UUID> studentIds,
        UUID classId,
        UUID academicTermId,
        LocalDate date,
        LocalDate startDate,
        LocalDate endDate,
        AttendanceStatus status
) {
}
