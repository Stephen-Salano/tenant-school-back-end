package io.psyllabus.platform.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Used by POST /api/users/me/change-password.
 * Both the current password (to verify identity) and the new password are required.
 *
 * K-DPA note: password changes are an audit event. AuditingConfig captures
 * the actor (currentUser) and timestamp via @LastModifiedBy / @LastModifiedDate.
 */
public record ChangePasswordRequest (
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "New password must be at least 8 characters")
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        String confirmPassword
){
}
