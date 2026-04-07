package io.psyllabus.platform.academic.controller;

import io.psyllabus.platform.academic.dto.request.SubjectCategoryCreateRequest;
import io.psyllabus.platform.academic.dto.response.SubjectCategoryResponse;
import io.psyllabus.platform.academic.service.SubjectCategoryService;
import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST facade for subject categories.
 */
@RestController
@RequestMapping("/api/subject-categories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Academic - Subject Categories", description = "Manage subject requirement types (Core, Elective, etc.)")
public class SubjectCategoryController extends BaseController {

    private final SubjectCategoryService subjectCategoryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new subject category")
    public ResponseEntity<ApiResponse<SubjectCategoryResponse>> createCategory(
            @Valid @RequestBody SubjectCategoryCreateRequest request) {
        log.info("POST /api/subject-categories — name={} tenant={}", request.name(), getCurrentTenantId());
        return created(subjectCategoryService.createCategory(request), "Subject category created successfully");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "List all subject categories for the tenant")
    public ResponseEntity<ApiResponse<List<SubjectCategoryResponse>>> listCategories() {
        return ok(subjectCategoryService.listCategories(), "Subject categories retrieved");
    }
}
