package io.psyllabus.platform.guardian.dto.request;

import io.psyllabus.platform.guardian.enums.RelationshipType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GuardianCreateRequest(
        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Phone is required")
        String phone,

        @Email(message = "Invalid email format")
        String email,

        @NotNull(message = "Relationship type is required")
        RelationshipType relationshipType,

        Boolean isFeePayer
) {
}
