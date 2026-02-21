package io.psyllabus.platform.school.controller;

import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.psyllabus.platform.school.dto.request.SchoolCreateRequest;
import io.psyllabus.platform.school.dto.responses.SchoolOnboardResponse;
import io.psyllabus.platform.school.dto.responses.SchoolResponse;
import io.psyllabus.platform.school.service.SchoolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * SchoolController — Facade pattern.
 *
 * This controller delegates entirely to SchoolService. It knows nothing about:
 *   - How Subscription is created
 *   - How the Branch is set up
 *   - How the admin User is registered
 *   - What TenantContext is
 *
 * That's SchoolServiceImpl's job. Controller = thin HTTP adapter.
 *
 * Extends BaseController (Template Method) — inherits getCurrentUser(),
 * getCurrentTenantId(), ok(), created(), okMessage().
 */

@RestController
@RequestMapping("/api/schools")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Schools", description = "School provisioning and management")
public class SchoolController extends BaseController {

    private final SchoolService schoolService;

    /**
     * POST /api/schools/onboard — PUBLIC (no JWT required).
     *
     * Platform admin calls this after closing a deal with a school.
     * Creates: School → Subscription → Branch → Admin user.
     * Returns one-time admin credentials — store them safely before returning to the school.
     *
     * SecurityConfig permits this path without authentication.
     */
    @PostMapping("/onboard")
    @Operation(summary = "Provision a new school", description = "Public endpoint - creates school, subscription, branch, and admin user in one transaction")
    public ResponseEntity<ApiResponse<SchoolOnboardResponse>> onboard(
            @Valid
            @RequestBody SchoolCreateRequest request
            ){
        log.info("School onboarding request: subdomain={}", request.subdomainPrefix());
        SchoolOnboardResponse response = schoolService.onboardSchool(request);
        log.info("School provisioned successfully: id={}", response.schoolId());
        return created(response, "School provisioned successfully");
    }

    /**
     * GET /api/schools/me — ADMIN only.
     *
     * Authenticated school admin fetches their own school profile.
     * Tenant-scoped — the admin can only see their own school.
     *
     * Note: we use the current user's tenantId (not a path variable) because
     * an admin should never be able to request a different school's data by ID.
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get current school profile")
    public ResponseEntity<ApiResponse<SchoolResponse>> getMySchool(Authentication authentication){
        User currentUser = getCurrentUser(authentication);
        log.debug("Fetching school profile for tenant={}", currentUser.getTenantId());
        SchoolResponse response = schoolService.findById(currentUser.getTenantId());
        return ok(response, "School profile retrieved");
    }

}
