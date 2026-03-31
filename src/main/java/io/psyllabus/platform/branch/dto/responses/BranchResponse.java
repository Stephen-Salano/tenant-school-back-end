package io.psyllabus.platform.branch.dto.responses;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Basic response for branch operations.
 */
public record BranchResponse(
    UUID id,
    String name,
    String code,
    String address,
    String phone,
    UUID branchAdminId,
    String branchAdminName,
    boolean isMainBranch,
    boolean isActive,
    LocalDateTime createdAt
) {
}
