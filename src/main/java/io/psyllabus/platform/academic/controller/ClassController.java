package io.psyllabus.platform.academic.controller;

import io.psyllabus.platform.academic.dto.request.AssignClassTeacherRequest;
import io.psyllabus.platform.academic.dto.request.ClassCreateRequest;
import io.psyllabus.platform.academic.dto.request.ClassUpdateRequest;
import io.psyllabus.platform.academic.dto.response.ClassDetailResponse;
import io.psyllabus.platform.academic.dto.response.ClassEnrollmentStatsResponse;
import io.psyllabus.platform.academic.dto.response.ClassResponse;
import io.psyllabus.platform.academic.service.ClassService;
import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.common.util.PaginationUtils;
import io.psyllabus.platform.student.dto.responses.StudentResponse;
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
 * REST facade for school class management.
 *
 * <p>All mutating endpoints require ADMIN role. List and detail endpoints allow STAFF
 * so teachers can view their class information and enrollment stats.</p>
 *
 * <p>{@code GET /api/classes/streams} returns the school-level configured stream names — useful
 * for the frontend to populate stream dropdowns without a separate profile fetch.</p>
 */
@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Academic - Classes", description = "Manage school classes (sections) within academic years")
public class ClassController extends BaseController {

    private final ClassService classService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Create a new class",
            description = "streamName is validated against school profile config. Use GET /api/classes/streams first to see valid values."
    )
    public ResponseEntity<ApiResponse<ClassResponse>> createClass(
            @Valid @RequestBody ClassCreateRequest request) {
        log.info("POST /api/classes — name={} year={} tenant={}", request.name(), request.academicYearId(), getCurrentTenantId());
        return created(classService.createClass(request), "Class created successfully");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(
            summary = "List classes by academic year (paginated)",
            description = "yearId is required. Returns classes scoped to the current tenant."
    )
    public ResponseEntity<ApiResponse<PageResponse<ClassResponse>>> getClassesByYear(
            @RequestParam UUID yearId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "gradeLevel") String sortBy) {
        Pageable pageable = PaginationUtils.of(page, size, sortBy);
        return paged(classService.getClassesByYear(yearId, pageable), "Classes retrieved");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get class detail with enrollment counts")
    public ResponseEntity<ApiResponse<ClassDetailResponse>> getClassById(@PathVariable UUID id) {
        return ok(classService.getClassById(id), "Class retrieved");
    }

    @GetMapping("/streams")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(
            summary = "List valid stream names for this school",
            description = "Returns the stream names configured in SchoolProfile. Empty list means no stream restriction."
    )
    public ResponseEntity<ApiResponse<List<String>>> getStreamNames() {
        return ok(classService.getStreamNames(), "Stream names retrieved");
    }

    @PutMapping("/{id}/class-teacher")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Assign a class (homeroom) teacher",
            description = "Staff must already have an active TeacherAssignment in this class. " +
                    "Previous class teacher is automatically demoted."
    )
    public ResponseEntity<ApiResponse<Void>> assignClassTeacher(
            @PathVariable UUID id,
            @Valid @RequestBody AssignClassTeacherRequest request) {
        log.info("PUT /api/classes/{}/class-teacher — staffId={} tenant={}", id, request.staffId(), getCurrentTenantId());
        classService.assignClassTeacher(id, request.staffId());
        return okMessage("Class teacher assigned successfully");
    }

    @GetMapping("/{id}/enrollment-stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get enrollment statistics for a class")
    public ResponseEntity<ApiResponse<ClassEnrollmentStatsResponse>> getEnrollmentStats(@PathVariable UUID id) {
        return ok(classService.getClassEnrollmentStats(id), "Enrollment stats retrieved");
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update class info",
            description = "Updates ONLY the fields provided in the request body.")
    public ResponseEntity<ApiResponse<ClassResponse>> updateClass(
            @PathVariable UUID id,
            @Valid @RequestBody ClassUpdateRequest request) {
        log.info("PATCH /api/classes/{} — tenant={}", id, getCurrentTenantId());
        return ok(classService.updateClass(id, request), "Class updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft delete class",
            description = "Sets status to INACTIVE.")
    public ResponseEntity<ApiResponse<Void>> deleteClass(@PathVariable UUID id) {
        log.info("DELETE /api/classes/{} — tenant={}", id, getCurrentTenantId());
        classService.deleteClass(id);
        return okMessage("Class deleted successfully");
    }

    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get roster of active students for a class")
    public ResponseEntity<ApiResponse<List<StudentResponse>>> getClassRoster(@PathVariable UUID id) {
        return ok(classService.getClassRoster(id), "Class roster retrieved");
    }
}
