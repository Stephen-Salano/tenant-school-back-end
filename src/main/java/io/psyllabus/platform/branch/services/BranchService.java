package io.psyllabus.platform.branch.services;

import io.psyllabus.platform.branch.dto.request.BranchCreateRequest;
import io.psyllabus.platform.branch.dto.responses.BranchDetailResponse;
import io.psyllabus.platform.branch.dto.responses.BranchResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for branch lifecycle management.
 * Enforces multi-tenancy and subscription limits.
 */
public interface BranchService {

    /**
     * Provisions a new school branch and creates its administrator user.
     * Only accessible to SCHOOL-scoped admins.
     *
     * @param request The branch details and initial admin info.
     * @return The created branch details.
     */
    BranchResponse createBranch(BranchCreateRequest request);

    /**
     * Retrieves all branches belonging to the current tenant.
     * Result set is filtered based on the current user's access scope.
     *
     * @return List of branch responses.
     */
    List<BranchResponse> getAllBranches();

    /**
     * Fetches details of a specific branch.
     * Access is strictly guarded by the user's scope and tenant ID.
     *
     * @param branchId The ID of the branch.
     * @return Detailed branch information.
     */
    BranchDetailResponse getBranchById(UUID branchId);

    /**
     * Toggles the active status of a branch.
     *
     * @param branchId The ID of the branch.
     * @param active The target status.
     */
    void setBranchStatus(UUID branchId, boolean active);

    /**
     * Deletes a branch if it has no associated data (students, staff, etc.).
     * Use with caution.
     *
     * @param branchId The ID of the branch to remove.
     */
    void deleteBranch(UUID branchId);
}
