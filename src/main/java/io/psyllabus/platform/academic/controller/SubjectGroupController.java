package io.psyllabus.platform.academic.controller;

import io.psyllabus.platform.academic.dto.request.AssignHodRequest;
import io.psyllabus.platform.academic.dto.request.SubjectGroupRequest;
import io.psyllabus.platform.academic.dto.response.SubjectGroupResponse;
import io.psyllabus.platform.academic.service.SubjectGroupService;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for managing Subject Groups (Curriculum Areas).
 * Scoped to the current tenant.
 */
@RestController
@RequestMapping("/api/academic/subject-groups")
@RequiredArgsConstructor
@Tag(name = "Academic - Subject Groups", description = "Endpoints for managing curriculum-neutral subject areas")
@PreAuthorize("hasRole('ADMIN')")
public class SubjectGroupController {

    private final SubjectGroupService subjectGroupService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new subject group")
    public ApiResponse<SubjectGroupResponse> create(@Valid @RequestBody SubjectGroupRequest request) {
        return ApiResponse.success(subjectGroupService.create(request), "Subject group created successfully");
    }

    @GetMapping
    @Operation(summary = "List all subject groups for the current school")
    public ApiResponse<Page<SubjectGroupResponse>> getAll(Pageable pageable) {
        return ApiResponse.success(subjectGroupService.getAll(pageable), "Subject groups retrieved successfully");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific subject group")
    public ApiResponse<SubjectGroupResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(subjectGroupService.getById(id), "Subject group retrieved successfully");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a subject group")
    public ApiResponse<SubjectGroupResponse> update(@PathVariable UUID id, @Valid @RequestBody SubjectGroupRequest request) {
        return ApiResponse.success(subjectGroupService.update(id, request), "Subject group updated successfully");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a subject group")
    public void delete(@PathVariable UUID id) {
        subjectGroupService.delete(id);
    }

    @PatchMapping("/{id}/hod")
    @Operation(summary = "Assign a Head of Department to a subject group")
    public ApiResponse<SubjectGroupResponse> assignHod(@PathVariable UUID id, @Valid @RequestBody AssignHodRequest request) {
        return ApiResponse.success(subjectGroupService.assignHod(id, request), "HOD assigned successfully");
    }

    @PatchMapping("/{id}/toggle-active")
    @Operation(summary = "Toggle a subject group's active status")
    public ApiResponse<Void> toggleActive(@PathVariable UUID id) {
        subjectGroupService.toggleActive(id);
        return ApiResponse.success(null, "Subject group status updated successfully");
    }
}
