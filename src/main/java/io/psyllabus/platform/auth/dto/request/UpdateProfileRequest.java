package io.psyllabus.platform.auth.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Partial update for the current user's profile.
 * All fields are optional — only non-null fields are applied (PATCH semantics).
 * Email and username are not updatable here (separate flow with verification needed).
 */
public record UpdateProfileRequest(
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Size(max = 20, message = "Phone must not exceed 20 characters")
        String phone
) {
}
