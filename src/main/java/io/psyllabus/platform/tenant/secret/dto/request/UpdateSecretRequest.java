package io.psyllabus.platform.tenant.secret.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSecretRequest(
    @NotBlank @Size(max = 500) String secretValue,
    @Size(max = 255) String description
) {}
