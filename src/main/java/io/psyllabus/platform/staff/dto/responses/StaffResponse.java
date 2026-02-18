package io.psyllabus.platform.staff.dto.responses;

import io.psyllabus.platform.staff.enums.EmploymentStatus;

import java.util.UUID;

/**
 * GET /api/staff
 * { id, name, employeeNumber, department, status }
 */
public record StaffResponse(
        UUID id,
        String employeeNumber,
        String firstName,
        String lastName,
        String email,
        EmploymentStatus employmentStatus
) {
}
