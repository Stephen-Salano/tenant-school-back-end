package io.psyllabus.platform.staff.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record StaffUpdateRequest(
        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName,

        @Size(max = 20)
        String phone,

        @Email
        @Size(max = 100)
        String email,

        UUID subjectGroupId,

        @Size(max = 255)
        String qualification,

        String specialization
) {
}
