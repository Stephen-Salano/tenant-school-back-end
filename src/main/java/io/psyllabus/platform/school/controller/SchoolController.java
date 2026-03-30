package io.psyllabus.platform.school.controller;

import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.psyllabus.platform.school.dto.request.SchoolProfileUpdateRequest;
import io.psyllabus.platform.school.dto.responses.SchoolProfileResponse;
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

import java.util.UUID;

/**
 * SchoolController — Facade pattern.
 *
 * <p>Delegates entirely to {@link SchoolService}. Contains no business logic,
 * no repository calls, and no tenant resolution. Extends {@link BaseController}
 * (Template Method) for {@code getCurrentUser()}, {@code ok()}, {@code created()}.</p>
 *
 * <p>{@code SchoolResponse} is retired — all school-admin-facing endpoints now
 * return {@link io.psyllabus.platform.school.dto.responses.SchoolProfileResponse}, which combines the lean identity fields
 * from the {@code schools} table with the full settings tabs from
 * {@code school_profiles}.</p>
 *
 * <p>{@code SchoolSummaryResponse} is preserved for platform-admin cross-tenant
 * endpoints in {@code PlatformAdminController}.</p>
 */
@RestController
@RequestMapping("/api/schools")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Schools", description = "School profile management for authenticated school admins")
public class SchoolController extends BaseController {

    private final SchoolService schoolService;

    /**
     * GET /api/schools/me — ADMIN only.
     *
     * <p>Returns the full school profile for the authenticated admin's school.
     * The school is resolved from {@code TenantContext} — the admin cannot
     * request a different school's profile by manipulating a path variable.</p>
     *
     * <p>Response is cached under {@code schoolProfiles:{schoolId}} for 10 minutes.
     * Cache is evicted when the admin saves any settings tab via PATCH.</p>
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get own school profile", description = "Returns the full school profile including all settings tabs. Cached for 10 minutes.")
    public ResponseEntity<ApiResponse<SchoolProfileResponse>> getMySchool(Authentication authentication) {
        UUID tenantId = getCurrentTenantId();
        log.debug("GET /api/schools/me — tenant={}", tenantId);
        return ok(schoolService.getSchoolProfile(tenantId), "School profile retrieved");
    }

    /**
     * PATCH /api/schools/me/profile — ADMIN only.
     *
     * <p>Partial update — only sections present in the request body are applied.
     * Omitting a section (sending it as {@code null}) leaves that settings tab untouched.
     * This matches the frontend's tab-by-tab save behaviour.</p>
     *
     * <p>The school is resolved from {@code TenantContext} — the {@code /me/} convention
     * prevents an admin from supplying a different school's ID in the path.</p>
     *
     * <p>Evicts the {@code schoolProfiles} cache entry for this school on success.</p>
     *
     * <p>Example — updating only the General tab:</p>
     * <pre>{@code
     * PATCH /api/schools/me/profile
     * {
     *   "general": {
     *     "motto": "Excellence Through Knowledge",
     *     "genderPolicy": "CO_EDUCATIONAL"
     *   }
     * }
     * }</pre>
     */
    @PatchMapping("/me/profile")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update own school profile",
            description = "Partial update — only provided sections are applied. Null sections are ignored."
    )
    public ResponseEntity<ApiResponse<SchoolProfileResponse>> updateMySchoolProfile(
            @Valid @RequestBody SchoolProfileUpdateRequest request,
            Authentication authentication) {
        UUID tenantId = getCurrentTenantId();
        log.info("PATCH /api/schools/me/profile — tenant={}", tenantId);
        return ok(schoolService.updateSchoolProfile(tenantId, request), "School profile updated");
    }

}
