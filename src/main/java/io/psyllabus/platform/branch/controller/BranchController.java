package io.psyllabus.platform.branch.controller;

import io.psyllabus.platform.branch.dto.request.BranchCreateRequest;
import io.psyllabus.platform.branch.dto.responses.BranchDetailResponse;
import io.psyllabus.platform.branch.dto.responses.BranchResponse;
import io.psyllabus.platform.branch.services.BranchService;
import io.psyllabus.platform.common.annotation.RequireFeature;
import io.psyllabus.platform.common.constants.FeatureConstants;
import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for branch/campus management.
 * Operations are tenant-scoped and feature-guarded.
 */
@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Branches", description = "Branch/Campus management for school administrators")
public class BranchController extends BaseController {

    private final BranchService branchService;

    /**
     * Create a new branch (campus).
     * Only accessible to SCHOOL-scoped admins.
     */
    @PostMapping
    @RequireFeature(FeatureConstants.MULTI_CAMPUS)
    @PreAuthorize("hasRole('ADMIN') and @scopeGuard.isSchoolAdmin()")
    @Operation(summary = "Create a new branch", description = "Provisions a new campus and its initial administrator. Requires SCHOOL-level admin scope.")
    public ResponseEntity<ApiResponse<BranchResponse>> createBranch(@Valid @RequestBody BranchCreateRequest request) {
        log.info("POST /api/v1/branches - Creating branch: {}", request.name());
        return created(branchService.createBranch(request), "Branch created successfully");
    }

    /**
     * List all branches for the current tenant.
     * Visibility is filtered based on the requester's AccessScope.
     */
    @GetMapping
    @Operation(summary = "List all branches", description = "Returns all branches for the current tenant. School admins see all, branch admins see only their own.")
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getAllBranches() {
        log.debug("GET /api/v1/branches - Fetching all branches");
        return ok(branchService.getAllBranches(), "Branches retrieved successfully");
    }

    /**
     * Fetch detailed information about a specific branch.
     * Guarded by ScopeGuard to ensure the user has permission for this branchId.
     */
    @GetMapping("/{id}")
    @PreAuthorize("@scopeGuard.canAccessBranch(#id)")
    @Operation(summary = "Get branch details", description = "Returns detailed information for a specific branch ID. Access is scope-guarded.")
    public ResponseEntity<ApiResponse<BranchDetailResponse>> getBranchById(
            @Parameter(description = "The UUID of the branch") @PathVariable UUID id) {
        log.debug("GET /api/v1/branches/{} - Fetching details", id);
        return ok(branchService.getBranchById(id), "Branch details retrieved successfully");
    }

    /**
     * Update branch status (Active/Inactive).
     * Only a SCHOOL-level admin can toggle branch availability.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') and @scopeGuard.isSchoolAdmin()")
    @Operation(summary = "Update branch status", description = "Activates or deactivates a branch. The main campus cannot be deactivated.")
    public ResponseEntity<ApiResponse<Void>> updateBranchStatus(
            @Parameter(description = "The UUID of the branch") @PathVariable UUID id,
            @Parameter(description = "New active status") @RequestParam boolean active) {
        log.info("PATCH /api/v1/branches/{}/status - Setting active={}", id, active);
        branchService.setBranchStatus(id, active);
        return okMessage("Branch status updated successfully");
    }
}
