package io.psyllabus.platform.student.dto.responses;

import java.util.UUID;

public record StudentSubjectResponse(
        UUID id,
        String name,
        String category,
        String teacherName
) {
}
