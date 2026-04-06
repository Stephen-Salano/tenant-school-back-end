package io.psyllabus.platform.guardian.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record LinkGuardianRequest(
        @NotNull UUID guardianId,
        boolean isPrimary
) {}
