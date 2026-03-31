package io.psyllabus.platform.branch.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new school branch.
 * Includes the branch details and the initial administrator account info.
 */
public record BranchCreateRequest(
    @NotBlank(message = "Branch name is required")
    @Size(max = 255, message = "Branch name cannot exceed 255 characters")
    String name,

    @NotBlank(message = "Branch code is required")
    @Size(max = 20, message = "Branch code cannot exceed 20 characters")
    String code,

    @Size(max = 1000, message = "Address cannot exceed 1000 characters")
    String address,

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    String phone,

    @Email(message = "Valid admin email is required")
    @NotBlank(message = "Admin email is required")
    String adminEmail,

    @NotBlank(message = "Admin first name is required")
    String adminFirstName,

    @NotBlank(message = "Admin last name is required")
    String adminLastName,

    @Size(max = 20, message = "Admin phone cannot exceed 20 characters")
    String adminPhone
) {
}
