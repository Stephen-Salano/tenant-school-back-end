package io.psyllabus.platform.branch.dto.responses;

import io.psyllabus.platform.branch.enums.BranchStatus;

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
    BranchStatus status,
    LocalDateTime createdAt
) {
}
