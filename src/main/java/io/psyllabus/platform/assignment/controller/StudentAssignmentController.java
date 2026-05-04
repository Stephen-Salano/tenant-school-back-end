package io.psyllabus.platform.assignment.controller;

import io.psyllabus.platform.assignment.dto.request.AssignmentSubmitRequest;
import io.psyllabus.platform.assignment.dto.response.StudentAssignmentDetailResponse;
import io.psyllabus.platform.assignment.dto.response.StudentAssignmentResponse;
import io.psyllabus.platform.assignment.enums.SubmissionStatus;
import io.psyllabus.platform.assignment.service.StudentAssignmentService;
import io.psyllabus.platform.common.annotation.RequireFeature;
import io.psyllabus.platform.common.constants.FeatureConstants;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/student/assignments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Student Assignments", description = "Student assignment listing and submission")
public class StudentAssignmentController extends BaseController {

    private final StudentAssignmentService studentAssignmentService;

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    @RequireFeature(FeatureConstants.ASSIGNMENTS)
    @Operation(summary = "List the current student's assignments")
    public ResponseEntity<ApiResponse<PageResponse<StudentAssignmentResponse>>> listAssignments(
            @RequestParam(required = false) SubmissionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Pageable pageable = PaginationUtils.of(page, size, "createdAt");
        return paged(
                studentAssignmentService.listAssignments(getCurrentUser(authentication).getId(), status, pageable),
                "Assignments retrieved"
        );
    }

    @GetMapping("/{assignmentId}")
    @PreAuthorize("hasRole('STUDENT')")
    @RequireFeature(FeatureConstants.ASSIGNMENTS)
    @Operation(summary = "Get assignment detail for the current student")
    public ResponseEntity<ApiResponse<StudentAssignmentDetailResponse>> getAssignment(
            @PathVariable UUID assignmentId,
            Authentication authentication) {
        return ok(studentAssignmentService.getAssignment(assignmentId, getCurrentUser(authentication).getId()), "Assignment retrieved");
    }

    @PostMapping("/{assignmentId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    @RequireFeature(FeatureConstants.ASSIGNMENTS)
    @Operation(summary = "Submit an assignment")
    public ResponseEntity<ApiResponse<StudentAssignmentDetailResponse>> submitAssignment(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody AssignmentSubmitRequest request,
            Authentication authentication) {
        return ok(
                studentAssignmentService.submitAssignment(assignmentId, request, getCurrentUser(authentication).getId()),
                "Assignment submitted"
        );
    }
}
