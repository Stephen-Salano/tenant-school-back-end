package io.psyllabus.platform.platformadmin.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/platform/admins}.
 *
 * <p>Creates a new platform-level administrator. Requires {@code PLATFORM} scope —
 * only existing platform admins can create other platform admins.</p>
 *
 * <p>K-DPA: platform admin accounts hold the highest privilege level in the system.
 * Creation of a new account is an auditable event. The raw password is encoded
 * before persistence and never stored in plaintext.</p>
 *
 * @param username   unique username for the new admin
 * @param email      unique email address
 * @param firstName  first name
 * @param lastName   last name
 * @param phone      contact phone (optional)
 * @param password   raw password — must meet minimum strength requirements
 */
public record PlatformAdminCreateRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        @Size(max = 150)
        String email,

        @NotBlank(message = "First name is required")
        @Size(max = 100)
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100)
        String lastName,

        @Size(max = 20)
        String phone,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {
}
