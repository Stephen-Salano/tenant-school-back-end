package io.psyllabus.platform.auth.dto.responses;

import java.util.UUID;

/**
 * Lightweight student summary embedded in guardian bootstrap claims.
 */
public record LinkedStudentClaim(
        UUID studentId,
        String admissionNumber,
        String firstName,
        String lastName,
        UUID currentClassId
) {
}
