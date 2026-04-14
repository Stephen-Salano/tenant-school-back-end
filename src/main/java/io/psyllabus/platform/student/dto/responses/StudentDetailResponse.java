package io.psyllabus.platform.student.dto.responses;

import io.psyllabus.platform.student.enums.Gender;
import io.psyllabus.platform.student.enums.StudentStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Detailed student view — returned by GET /api/students/{id} for ADMIN and STAFF only.
 *
 * K-DPA RULE: medicalConditions is sensitive personal data.
 * This DTO must NEVER be returned to PARENT role.
 * The controller enforces this via RBAC — PARENT requests are blocked entirely
 * until guardian-link verification is built (future sprint).
 *
 * List endpoints use StudentResponse (no medical data).
 */
public record StudentDetailResponse(
        UUID id,
        UUID userId,
        String admissionNumber,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        Gender gender,
        UUID currentClassId,
        LocalDate admissionDate,
        StudentStatus currentStatus,
        String medicalConditions   // K-DPA: ADMIN/STAFF only — never expose to PARENT
) {
}
