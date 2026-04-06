package io.psyllabus.platform.academic.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing a {@link io.psyllabus.platform.academic.entities.SubjectGroup}.
 */
public record SubjectGroupResponse(
    UUID id,
    UUID tenantId,
    String name,
    String description,
    UUID hodStaffId,
    boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
