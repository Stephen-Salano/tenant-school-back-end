package io.psyllabus.platform.academic.controller;

import io.psyllabus.platform.academic.dto.request.AcademicTermCreateRequest;
import io.psyllabus.platform.academic.dto.request.AcademicTermUpdateRequest;
import io.psyllabus.platform.academic.dto.response.AcademicTermDetailResponse;
import io.psyllabus.platform.academic.dto.response.AcademicTermResponse;
import io.psyllabus.platform.academic.service.AcademicTermService;
import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.common.util.PaginationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST facade for academic term management.
 */
@RestController
@RequestMapping("/api/academic/terms")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Academic - Terms", description = "Manage the school's academic terms")
@PreAuthorize("hasRole('ADMIN')")
public class AcademicTermController extends BaseController {

    private final AcademicTermService academicTermService;

    @PostMapping
    @Operation(summary = "Create a new academic term")
    public ResponseEntity<ApiResponse<AcademicTermResponse>> create(
            @Valid @RequestBody AcademicTermCreateRequest request) {
        log.info("POST /api/academic/terms — tenant={}", getCurrentTenantId());
        return created(academicTermService.create(request), "Academic term created");
    }

    @GetMapping
    @Operation(summary = "List all academic terms (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<AcademicTermResponse>>> listAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startDate") String sortBy) {
        Pageable pageable = PaginationUtils.of(page, size, sortBy);
        return paged(academicTermService.listAll(pageable), "Academic terms retrieved");
    }

    @GetMapping("/year/{yearId}")
    @Operation(summary = "List all academic terms for a specific year")
    public ResponseEntity<ApiResponse<List<AcademicTermResponse>>> listByYear(@PathVariable UUID yearId) {
        return ok(academicTermService.listByYear(yearId), "Academic terms for year retrieved");
    }

    @GetMapping("/current")
    @Operation(summary = "Get the current academic term")
    public ResponseEntity<ApiResponse<AcademicTermResponse>> getCurrent() {
        return ok(academicTermService.getCurrentTerm(), "Current academic term retrieved");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific academic term by ID")
    public ResponseEntity<ApiResponse<AcademicTermDetailResponse>> getById(@PathVariable UUID id) {
        return ok(academicTermService.getById(id), "Academic term retrieved");
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update an academic term (PATCH semantics)")
    public ResponseEntity<ApiResponse<AcademicTermResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody AcademicTermUpdateRequest request) {
        log.info("PATCH /api/academic/terms/{} — tenant={}", id, getCurrentTenantId());
        return ok(academicTermService.update(id, request), "Academic term updated");
    }

    @PutMapping("/{id}/set-current")
    @Operation(summary = "Set this term as the current academic term")
    public ResponseEntity<ApiResponse<AcademicTermResponse>> setCurrentTerm(@PathVariable UUID id) {
        log.info("PUT /api/academic/terms/{}/set-current — tenant={}", id, getCurrentTenantId());
        return ok(academicTermService.setCurrentTerm(id), "Current academic term updated");
    }
}
