package io.psyllabus.platform.branch.dto.responses;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Detailed branch response, including audit information.
 */
public record BranchDetailResponse(
    UUID id,
    String name,
    String code,
    String address,
    String phone,
    String email,
    UUID branchAdminId,
    String branchAdminName,
    boolean isMainBranch,
    boolean isActive,
    LocalDateTime createdAt,
    String createdBy
) {

}
