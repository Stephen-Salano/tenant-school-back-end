package io.psyllabus.platform.platformadmin.controller;

import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.platformadmin.dto.request.PlatformAdminCreateRequest;
import io.psyllabus.platform.platformadmin.dto.responses.PlatformAdminResponse;
import io.psyllabus.platform.platformadmin.entities.PlatformAdmin;
import io.psyllabus.platform.platformadmin.services.PlatformAdminService;
import io.psyllabus.platform.school.dto.request.SchoolCreateRequest;
import io.psyllabus.platform.school.dto.responses.SchoolOnboardResponse;
import io.psyllabus.platform.school.dto.responses.SchoolSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/platform")
@RequiredArgsConstructor
@Slf4j
@Tag(name="Platform Admin", description = "Cross-tenant platform administration - PLATFORM_ADMIN only")
@SecurityRequirement(name = "bearerAuth")
public class PlatformAdminController extends BaseController {

    private final PlatformAdminService platformAdminService;

    /**
     * Provision a new school end-to-end.
     *
     * <p>Replaces the retired {@code POST /api/schools/onboard} endpoint
     * That endpoint required only an {@code X-Platform-Key} header. This endpoint
     * additionally requires a full {@code PLATFORM_ADMIN} JWT.</p>
     *
     * <p>Supply exactly one of {@code subdomainPrefix} or {@code customDomain} in the
     * request body. Both or neither is a 400.</p>
     *
     * <p>K-DPA: the response contains a one-time plaintext temp password.
     * Must be called over HTTPS. The client must not cache the response.</p>
     */
    @PostMapping("/schools")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(
            summary = "Provision a new school",
            description = "Creates school, subscription, main branch, and initial admin account. Returns one-time temp credentials."
    )
    public ResponseEntity<ApiResponse<SchoolOnboardResponse>> provisionSchool(
            @Valid @RequestBody SchoolCreateRequest request,
            Authentication authentication
            ){
        UUID adminId = extractPlatformAdminId(authentication);
        log.info("School provision request: name='{}', plan={}, adminId={}",
                request.name(), request.planType(), adminId);

        SchoolOnboardResponse response = platformAdminService.provisionSchool(request, adminId);
        return created(response, "School provisioned successfully");
    }


    /**
     * List all schools across all tenants, paginated.
     */
    @GetMapping("/schools")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "List all schools", description = "Cross-tenant paginated school list")
    public ResponseEntity<ApiResponse<PageResponse<SchoolSummaryResponse>>> listAllSchools(
            @PageableDefault(size = 20, sort = "createdAt")Pageable pageable
            ){
        log.debug("List all schools: page={}", pageable.getPageNumber());
        Page<SchoolSummaryResponse> page = platformAdminService.listAllSchools(pageable);
        return paged(PageResponse.from(page), "Schools retrieved");
    }

    /**
     * Fetch any school's profile by ID, regardless of tenant.
     */
    @GetMapping("/schools/{schoolId}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Get any school", description = "Cross-tenant single school fetch")
    public ResponseEntity<ApiResponse<SchoolSummaryResponse>> getAnySchool(
            @PathVariable UUID schoolId
    ){
        log.debug("Get school: schoolId={}", schoolId);
        return ok(platformAdminService.getAnySchool(schoolId), "School retrieved");
    }

    /**
     * Create a new platform administrator.
     *
     * <p>Only existing platform admins can create other platform admins.
     * The new account is created with {@code ACTIVE} status and no tenant association.</p>
     */
    @PostMapping("/admins")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Create platform admin", description = "Create a new platform-level administrator account")
    public ResponseEntity<ApiResponse<PlatformAdminResponse>> createPlatformAdmin(
            @Valid @RequestBody PlatformAdminCreateRequest request,
            Authentication authentication
    ){
        UUID requestingAdminId = extractPlatformAdminId(authentication);
        log.info("Create platform admin: username={}, requestedBy={}",
                    request.username(), requestingAdminId
        );
        return created(platformAdminService.createPlatformAdmin(request), "Platform admin created");
    }


    // Helper function
    /**
     * Extract the platform admin's UUID from the {@code SecurityContext}.
     *
     * <p>{@code PlatformAdminAuthFilter} sets the {@link PlatformAdmin} entity as the
     * principal. If it is not, the filter chain has a bug — fail fast.</p>
     */
    private UUID extractPlatformAdminId(Authentication authentication) {
        if (authentication == null ||
                !(authentication.getPrincipal() instanceof PlatformAdmin admin)) {
            throw new IllegalStateException(
                    "Expected PlatformAdmin principal — check PlatformAdminAuthFilter");
        }
        return admin.getId();
    }
}
