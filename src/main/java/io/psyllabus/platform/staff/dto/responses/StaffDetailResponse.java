package io.psyllabus.platform.staff.dto.responses;

import io.psyllabus.platform.staff.enums.EmploymentStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Detailed staff view — returned by GET /api/staff/{id}.
 * Richer than StaffResponse (which is for list pages).
 *
 * RBAC: ADMIN and STAFF roles only.
 * branchId/branchName deferred — requires User join; added when branch controllers are built.
 */
public record StaffDetailResponse (
        UUID id,
        String employeeNumber,
        String firstName,
        String lastName,
        String email,
        String phone,
        UUID departmentId,
        LocalDate hireDate,
        EmploymentStatus employmentStatus,
        String qualification,
        String specialization
){
}
