package io.psyllabus.platform.school.dto.request;

import io.psyllabus.platform.subscription.enums.PlanType;
import jakarta.validation.constraints.*;

/**
 * Request to provision a new school (tenant).
 * Sent by the platform admin after closing a deal with a school.
 *
 * This is a public endpoint — no JWT required.
 * Validation is strict because this creates the root tenant record.
 */
public record SchoolCreateRequest(
        @NotBlank(message = "School name is required")
        @Size(max = 200, message = "School name must not exceed 200 characters")
        String name,

        @NotBlank(message = "Subdomain prefix is required")
        @Size(min = 3, max = 50, message = "Subdomain must be 3–50 characters")
        @Pattern(regexp = "^[a-z0-9-]{3,20}$", message = "Subdomain must be 3-20 characters, lowercase alphanumeric and hyphens only")
        String subdomainPrefix,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Phone number is required")
        String phone,

        @NotBlank(message = "Physical address is required")
        String address,

        @NotNull(message = "Plan type is required")
        PlanType planType,

        // ── Admin user details ──────────────────────────────────────────────
        // The first admin user is created during provisioning.
        // Platform admin hands these credentials to the school.
        @NotBlank(message = "Admin email is required")
        @Email(message = "Must be a valid email address")
        String adminEmail,

        @NotBlank(message = "Admin first name is required")
        String adminFirstName,

        @NotBlank(message = "Admin last name is required")
        String adminLastName,

        @NotBlank(message = "Admin phone is required")
        String adminPhone
) {
}
