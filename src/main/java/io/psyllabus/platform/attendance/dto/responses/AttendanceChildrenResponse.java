package io.psyllabus.platform.attendance.dto.responses;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AttendanceChildrenResponse(
        UUID guardianId,
        LocalDate startDate,
        LocalDate endDate,
        int childCount,
        List<StudentAttendanceResponse> children
) {
}
