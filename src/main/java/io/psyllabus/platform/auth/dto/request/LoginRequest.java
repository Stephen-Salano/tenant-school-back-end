package io.psyllabus.platform.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Username is required")
        String username, // can be username, email or admissionNumber

        @NotBlank(message = "Password is required")
        String password
) {
}
