package io.psyllabus.platform.attendance.dto.responses;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StudentAttendanceResponse(
        UUID studentId,
        String admissionNumber,
        String studentName,
        LocalDate startDate,
        LocalDate endDate,
        AttendanceReportResponse summary,
        List<AttendanceResponse> records
) {
}
