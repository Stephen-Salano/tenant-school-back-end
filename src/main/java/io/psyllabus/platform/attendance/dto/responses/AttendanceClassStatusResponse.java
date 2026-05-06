package io.psyllabus.platform.attendance.dto.responses;

import java.time.LocalDate;
import java.util.UUID;

public record AttendanceClassStatusResponse(
        UUID classId,
        String className,
        LocalDate date,
        boolean marked,
        long markedCount,
        long totalStudents
) {
}
