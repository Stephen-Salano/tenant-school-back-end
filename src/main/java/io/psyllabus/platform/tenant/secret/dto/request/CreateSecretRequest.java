package io.psyllabus.platform.tenant.secret.dto.request;

import io.psyllabus.platform.tenant.secret.enums.SecretType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSecretRequest(
    @NotNull SecretType secretType,
    @NotBlank @Size(max = 500) String secretValue,
    @Size(max = 255) String description
) {}
