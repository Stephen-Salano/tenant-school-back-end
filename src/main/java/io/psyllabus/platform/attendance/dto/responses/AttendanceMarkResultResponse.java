package io.psyllabus.platform.attendance.dto.responses;

import java.time.LocalDate;
import java.util.UUID;

public record AttendanceMarkResultResponse(
        UUID classId,
        String className,
        LocalDate date,
        int markedCount,
        String idempotencyKey,
        boolean idempotent
) {
}
