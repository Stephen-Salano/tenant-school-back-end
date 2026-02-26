package io.psyllabus.platform.staff.controller;

import io.psyllabus.platform.common.annotation.RequireFeature;
import io.psyllabus.platform.common.constants.FeatureConstants;
import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.common.util.PaginationUtils;
import io.psyllabus.platform.staff.dto.request.StaffCreateRequest;
import io.psyllabus.platform.staff.dto.request.UpdateStaffStatusRequest;
import io.psyllabus.platform.staff.dto.responses.StaffDetailResponse;
import io.psyllabus.platform.staff.dto.responses.StaffResponse;
import io.psyllabus.platform.staff.services.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * StaffController — Facade over StaffService.
 *
 * Knows nothing about: how Users are created, how tenant isolation works,
 * what EmploymentStatus transitions mean. All that is StaffServiceImpl's job.
 *
 * RBAC:
 *   POST   → ADMIN only
 *   GET    → ADMIN or STAFF (staff can view colleagues)
 *   PATCH  → ADMIN only (status transitions are administrative)
 */

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Staff", description = "Staff management — create, kist, and manage employment status")
public class StaffController extends BaseController {

    private final StaffService staffService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @RequireFeature(FeatureConstants.STAFF_MANAGEMENT)
    @Operation(summary = "Create a new staff member",
            description = "Creates a user account + Staff record. Default password = employee number .")
    public ResponseEntity<ApiResponse<StaffResponse>> createStaff(
            @Valid @RequestBody StaffCreateRequest request
            ){
        log.info("Creating staff: employeeNumber={}", request.employeeNumber());
        return created(staffService.createStaff(request), "Staff member created successfully");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @RequireFeature(FeatureConstants.STAFF_MANAGEMENT)
    @Operation(summary = "List all staff (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<StaffResponse>>> listStaff(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ){
        Pageable pageable = PaginationUtils.of(page, size, sortBy);
        return paged(staffService.findAll(pageable), "Staff retrieved");
    }

    @GetMapping("/{staffId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @RequireFeature(FeatureConstants.STAFF_MANAGEMENT)
    @Operation(summary = "Get staff memeber details")
    public ResponseEntity<ApiResponse<StaffDetailResponse>> getStaff(
            @PathVariable UUID staffId
            ){
        return ok(staffService.findById(staffId), "Staff retrieved");
    }

    @PatchMapping("/{staffId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @RequireFeature(FeatureConstants.STAFF_MANAGEMENT)
    @Operation(summary = "Update staff employment status",
    description = "Transitions employment status (ACTIVE → ON_LEAVE, TERMINATED, etc.). Reason is optional audit context.")
    public ResponseEntity<ApiResponse<StaffResponse>> updateStatus(
            @PathVariable UUID staffId,
            @Valid @RequestBody UpdateStaffStatusRequest request
            ){
        log.info("Updating staff {} status -> {}", staffId, request.status());
        return ok(staffService.updateStatus(staffId, request), "Staff Status updated");
    }
}
