package io.psyllabus.platform.academic.controller;

import io.psyllabus.platform.academic.dto.request.SubjectCreateRequest;
import io.psyllabus.platform.academic.dto.response.SubjectResponse;
import io.psyllabus.platform.academic.service.SubjectService;
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
 * REST facade for school subjects.
 */
@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Academic - Subjects", description = "Manage school subjects (Curriculum Items)")
public class SubjectController extends BaseController {

    private final SubjectService subjectService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new subject")
    public ResponseEntity<ApiResponse<SubjectResponse>> createSubject(
            @Valid @RequestBody SubjectCreateRequest request) {
        log.info("POST /api/subjects — name={} code={} tenant={}", request.name(), request.code(), getCurrentTenantId());
        return created(subjectService.createSubject(request), "Subject created successfully");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "List all subjects for the tenant (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<SubjectResponse>>> listSubjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy) {
        Pageable pageable = PaginationUtils.of(page, size, sortBy);
        return paged(subjectService.listSubjects(pageable), "Subjects retrieved");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get subject detail by ID")
    public ResponseEntity<ApiResponse<SubjectResponse>> getSubjectById(@PathVariable UUID id) {
        return ok(subjectService.getSubjectById(id), "Subject retrieved");
    }
}
