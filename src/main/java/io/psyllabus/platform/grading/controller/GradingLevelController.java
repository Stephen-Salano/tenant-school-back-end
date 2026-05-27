package io.psyllabus.platform.grading.controller;

import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.psyllabus.platform.grading.dto.request.GradingLevelCreateRequest;
import io.psyllabus.platform.grading.dto.request.GradingLevelUpdateRequest;
import io.psyllabus.platform.grading.dto.response.GradingLevelResponse;
import io.psyllabus.platform.grading.service.GradingLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/grading/levels")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Grading Levels", description = "Manage grading levels within a grading scheme")
@PreAuthorize("hasRole('ADMIN')")
public class GradingLevelController extends BaseController {

    private final GradingLevelService gradingLevelService;

    @PostMapping
    @Operation(summary = "Create a grading level within a scheme")
    public ResponseEntity<ApiResponse<GradingLevelResponse>> create(
            @RequestParam UUID schemeId,
            @Valid @RequestBody GradingLevelCreateRequest request) {
        log.info("Create grading level: scheme={} tenant={}", schemeId, getCurrentTenantId());
        return created(gradingLevelService.createLevel(schemeId, request), "Grading level created");
    }

    @GetMapping
    @Operation(summary = "List all grading levels for a scheme")
    public ResponseEntity<ApiResponse<List<GradingLevelResponse>>> listByScheme(
            @RequestParam UUID schemeId) {
        log.info("List grading levels: scheme={} tenant={}", schemeId, getCurrentTenantId());
        return ok(gradingLevelService.listByScheme(schemeId), "Grading levels retrieved");
    }

    @GetMapping("/{levelId}")
    @Operation(summary = "Get a grading level by ID")
    public ResponseEntity<ApiResponse<GradingLevelResponse>> getById(@PathVariable UUID levelId) {
        log.info("Get grading level: {} tenant={}", levelId, getCurrentTenantId());
        return ok(gradingLevelService.getById(levelId), "Grading level retrieved");
    }

    @PatchMapping("/{levelId}")
    @Operation(summary = "Update a grading level")
    public ResponseEntity<ApiResponse<GradingLevelResponse>> update(
            @PathVariable UUID levelId,
            @Valid @RequestBody GradingLevelUpdateRequest request) {
        log.info("Update grading level: {} tenant={}", levelId, getCurrentTenantId());
        return ok(gradingLevelService.updateLevel(levelId, request), "Grading level updated");
    }

    @DeleteMapping("/{levelId}")
    @Operation(summary = "Delete a grading level")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID levelId) {
        log.info("Delete grading level: {} tenant={}", levelId, getCurrentTenantId());
        gradingLevelService.deleteLevel(levelId);
        return okMessage("Grading level deleted");
    }
}