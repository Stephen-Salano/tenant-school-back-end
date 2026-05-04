package io.psyllabus.platform.grading.controller;

import io.psyllabus.platform.common.annotation.RequireFeature;
import io.psyllabus.platform.common.constants.FeatureConstants;
import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.common.util.PaginationUtils;
import io.psyllabus.platform.grading.dto.request.GradingSchemeCreateRequest;
import io.psyllabus.platform.grading.dto.request.GradingSchemeStatusUpdateRequest;
import io.psyllabus.platform.grading.dto.request.GradingSchemeUpdateRequest;
import io.psyllabus.platform.grading.dto.response.GradingSchemeDetailResponse;
import io.psyllabus.platform.grading.dto.response.GradingSchemeResponse;
import io.psyllabus.platform.grading.service.GradingSchemeService;
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

@RestController
@RequestMapping("/api/grading/schemes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Grading Schemes", description = "Tenant-scoped grading scheme management")
public class GradingSchemeController extends BaseController {

    private final GradingSchemeService gradingSchemeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @RequireFeature(FeatureConstants.GRADES)
    @Operation(summary = "Create a grading scheme with inline levels")
    public ResponseEntity<ApiResponse<GradingSchemeDetailResponse>> createScheme(
            @Valid @RequestBody GradingSchemeCreateRequest request) {
        return created(gradingSchemeService.createScheme(request), "Grading scheme created");
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @RequireFeature(FeatureConstants.GRADES)
    @Operation(summary = "List grading schemes")
    public ResponseEntity<ApiResponse<PageResponse<GradingSchemeResponse>>> listSchemes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PaginationUtils.of(page, size, "createdAt");
        return paged(gradingSchemeService.listSchemes(pageable), "Grading schemes retrieved");
    }

    @GetMapping("/{schemeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @RequireFeature(FeatureConstants.GRADES)
    @Operation(summary = "Get grading scheme detail")
    public ResponseEntity<ApiResponse<GradingSchemeDetailResponse>> getScheme(@PathVariable UUID schemeId) {
        return ok(gradingSchemeService.getScheme(schemeId), "Grading scheme retrieved");
    }

    @PatchMapping("/{schemeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @RequireFeature(FeatureConstants.GRADES)
    @Operation(summary = "Patch a grading scheme and its inline levels")
    public ResponseEntity<ApiResponse<GradingSchemeDetailResponse>> updateScheme(
            @PathVariable UUID schemeId,
            @Valid @RequestBody GradingSchemeUpdateRequest request) {
        return ok(gradingSchemeService.updateScheme(schemeId, request), "Grading scheme updated");
    }

    @PatchMapping("/{schemeId}/default")
    @PreAuthorize("hasRole('ADMIN')")
    @RequireFeature(FeatureConstants.GRADES)
    @Operation(summary = "Switch the tenant default grading scheme")
    public ResponseEntity<ApiResponse<GradingSchemeDetailResponse>> setDefault(@PathVariable UUID schemeId) {
        return ok(gradingSchemeService.setDefaultScheme(schemeId), "Default grading scheme updated");
    }

    @PatchMapping("/{schemeId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @RequireFeature(FeatureConstants.GRADES)
    @Operation(summary = "Activate or deactivate a grading scheme")
    public ResponseEntity<ApiResponse<GradingSchemeDetailResponse>> updateStatus(
            @PathVariable UUID schemeId,
            @Valid @RequestBody GradingSchemeStatusUpdateRequest request) {
        return ok(gradingSchemeService.updateStatus(schemeId, request), "Grading scheme status updated");
    }
}
