package io.psyllabus.platform.attendance.dto.responses;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ClassAttendanceResponse(
        UUID classId,
        String className,
        LocalDate date,
        boolean alreadyMarked,
        AttendanceReportResponse summary,
        List<AttendanceResponse> records
) {
}
