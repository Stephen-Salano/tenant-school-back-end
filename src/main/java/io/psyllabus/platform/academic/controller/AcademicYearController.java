package io.psyllabus.platform.academic.controller;

import io.psyllabus.platform.academic.dto.request.AcademicYearCreateRequest;
import io.psyllabus.platform.academic.dto.request.AcademicYearUpdateRequest;
import io.psyllabus.platform.academic.dto.response.AcademicYearDetailResponse;
import io.psyllabus.platform.academic.dto.response.AcademicYearResponse;
import io.psyllabus.platform.academic.service.AcademicYearService;
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

import java.util.UUID;

/**
 * REST facade for academic year management.
 *
 * <p>All endpoints are restricted to {@code ADMIN} role — academic calendar setup
 * is an administrative function available on all subscription plans.</p>
 *
 * <p>{@code PUT /{id}/set-current} is the only endpoint that triggers optimistic
 * locking logic. A concurrent conflict returns HTTP 409; the client should retry.</p>
 */
@RestController
@RequestMapping("/api/academic/years")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Academic - Years", description = "Manage the school's academic year calendar")
@PreAuthorize("hasRole('ADMIN')")
public class AcademicYearController extends BaseController {

    private final AcademicYearService academicYearService;

    @PostMapping
    @Operation(summary = "Create a new academic year",
            description = "Name must be unique within the school. Start date must be before end date.")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> create(
            @Valid @RequestBody AcademicYearCreateRequest request) {
        log.info("POST /api/academic/years — tenant={}", getCurrentTenantId());
        return created(academicYearService.create(request), "Academic year created");
    }

    @GetMapping
    @Operation(summary = "List all academic years (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<AcademicYearResponse>>> listAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startDate") String sortBy) {
        Pageable pageable = PaginationUtils.of(page, size, sortBy);
        return paged(academicYearService.listAll(pageable), "Academic years retrieved");
    }

    @GetMapping("/current")
    @Operation(summary = "Get the current academic year")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> getCurrent() {
        return ok(academicYearService.getCurrentYear(), "Current academic year retrieved");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific academic year by ID")
    public ResponseEntity<ApiResponse<AcademicYearDetailResponse>> getById(@PathVariable UUID id) {
        return ok(academicYearService.getById(id), "Academic year retrieved");
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update an academic year (PATCH semantics — null fields are ignored)")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody AcademicYearUpdateRequest request) {
        log.info("PATCH /api/academic/years/{} — tenant={}", id, getCurrentTenantId());
        return ok(academicYearService.update(id, request), "Academic year updated");
    }

    @PutMapping("/{id}/set-current")
    @Operation(summary = "Set this year as the current academic year",
            description = "Clears the previous current year atomically. Returns HTTP 409 if a concurrent " +
                          "modification is detected — client should retry.")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> setCurrentYear(@PathVariable UUID id) {
        log.info("PUT /api/academic/years/{}/set-current — tenant={}", id, getCurrentTenantId());
        return ok(academicYearService.setCurrentYear(id), "Current academic year updated");
    }
}
