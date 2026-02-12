package io.psyllabus.platform.student.dto.request;

import io.psyllabus.platform.student.enums.Gender;
import io.psyllabus.platform.student.enums.StudentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record StudentCreateRequest(
        @NotBlank(message = "Admission number is required")
        String admissionNumber,

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotNull(message = "Date of birth is required")
        LocalDate dateOfBirth,

        @NotNull(message = "Gender is required")
        Gender gender,

        @Email(message = "Invalid email format")
        String email,

        String phone,

        UUID branchId,

        UUID classId,

        StudentStatus status,

        String medicalConditions
) {
}
