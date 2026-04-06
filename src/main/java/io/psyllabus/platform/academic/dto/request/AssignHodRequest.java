package io.psyllabus.platform.academic.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO for assigning a Head of Department to a Subject Group.
 */
public record AssignHodRequest(
    @NotNull(message = "Staff ID is required")
    UUID staffId
) {}
