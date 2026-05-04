package io.psyllabus.platform.school.dto.request;

import io.psyllabus.platform.school.enums.SchoolStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for updating a school's status.
 * Allows for specifying the new status and an optional reason for auditing.
 */
public record SchoolStatusUpdateRequest(
    @NotNull(message = "School status is required")
    SchoolStatus status,

    String reason
) {}
