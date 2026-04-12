package io.psyllabus.platform.staff.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record StaffCreateRequest(
        @NotBlank(message = "Employee number is required")
        String employeeNumber,

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        String phone,

        UUID subjectGroupId,

        @NotNull(message = "Hire date is required")
        LocalDate hireDate,

        String qualification,

        String specialization,

        UUID branchId
) {
}
