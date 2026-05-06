package io.psyllabus.platform.attendance.dto.responses;

public record AttendanceReportResponse(
        long totalRecords,
        long presentCount,
        long lateCount,
        long absentCount,
        long excusedCount,
        double attendanceRate
) {
}
