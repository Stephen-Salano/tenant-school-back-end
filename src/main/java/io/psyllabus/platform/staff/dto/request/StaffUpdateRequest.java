package io.psyllabus.platform.staff.dto.request;

import io.psyllabus.platform.staff.enums.EmploymentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record StaffUpdateRequest(
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Email(message = "Invalid email format")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,

        @Size(max = 20, message = "Phone must not exceed 20 characters")
        String phone,

        UUID subjectGroupId,

        @PastOrPresent(message = "Hire date cannot be in the future")
        LocalDate hireDate,

        EmploymentStatus employmentStatus,

        @Size(max = 255, message = "Qualification must not exceed 255 characters")
        String qualification,

        String specialization,

        UUID branchId
) {
}
