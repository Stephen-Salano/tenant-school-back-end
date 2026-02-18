package io.psyllabus.platform.school.dto.request;

import io.psyllabus.platform.subscription.enums.PlanType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SchoolCreateRequest(
        @NotBlank(message = "School name is required")
        String name,

        @NotBlank(message = "Subdomain prefix is required")
        @Pattern(regexp = "^[a-z0-9-]{3,20}$", message = "Subdomain must be 3-20 characters, lowercase alphanumeric and hyphens only")
        String subdomainPrefix,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        String phone,

        String address,

        @NotNull(message = "Plan type is required")
        PlanType planType,

        // Admin user details
        // TODO: IS there a need for these in the create request ??
        @NotBlank(message = "Admin first name is required")
        String adminFirstName,

        @NotBlank(message = "Admin last name is required")
        String adminLastName,

        @NotBlank(message = "Admin email is required")
        @Email(message = "Invalid admin email format")
        String adminEmail,

        String adminPhone
) {
}
