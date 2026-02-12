package io.psyllabus.platform.student.dto.responses;

import io.psyllabus.platform.student.enums.StudentStatus;

import java.util.UUID;

public record StudentResponse(
        UUID id,
        String admissionNumber,
        String firstName,
        String lastName,
        StudentStatus status
) {
}
