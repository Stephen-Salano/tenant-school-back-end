package io.psyllabus.platform.assignment.controller;

import io.psyllabus.platform.assignment.dto.request.AssignmentCreateRequest;
import io.psyllabus.platform.assignment.dto.request.AssignmentUpdateRequest;
import io.psyllabus.platform.assignment.dto.request.BulkGradeRequest;
import io.psyllabus.platform.assignment.dto.request.GradeSubmissionRequest;
import io.psyllabus.platform.assignment.dto.response.AssignmentDetailResponse;
import io.psyllabus.platform.assignment.dto.response.AssignmentResponse;
import io.psyllabus.platform.assignment.dto.response.PendingSubmissionResponse;
import io.psyllabus.platform.assignment.dto.response.SubmissionResponse;
import io.psyllabus.platform.assignment.enums.AssignmentStatus;
import io.psyllabus.platform.assignment.enums.SubmissionStatus;
import io.psyllabus.platform.assignment.service.AssignmentService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Assignments", description = "Teacher assignment and grading workflows")
public class AssignmentController extends BaseController {

    private final AssignmentService assignmentService;

    @PostMapping
    @PreAuthorize("hasRole('STAFF')")
    @RequireFeature(FeatureConstants.ASSIGNMENTS)
    @Operation(summary = "Create a draft assignment")
    public ResponseEntity<ApiResponse<AssignmentDetailResponse>> createAssignment(
            @Valid @RequestBody AssignmentCreateRequest request,
            Authentication authentication) {
        return created(assignmentService.createAssignment(request, getCurrentUser(authentication).getId()), "Assignment created");
    }

    @GetMapping
    @PreAuthorize("hasRole('STAFF')")
    @RequireFeature(FeatureConstants.ASSIGNMENTS)
    @Operation(summary = "List teacher assignments")
    public ResponseEntity<ApiResponse<PageResponse<AssignmentResponse>>> listAssignments(
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) AssignmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Pageable pageable = PaginationUtils.of(page, size, "createdAt");
        return paged(
                assignmentService.listAssignments(getCurrentUser(authentication).getId(), classId, status, pageable),
                "Assignments retrieved"
        );
    }

    @GetMapping("/{assignmentId}")
    @PreAuthorize("hasRole('STAFF')")
    @RequireFeature(FeatureConstants.ASSIGNMENTS)
    @Operation(summary = "Get assignment detail")
    public ResponseEntity<ApiResponse<AssignmentDetailResponse>> getAssignment(
            @PathVariable UUID assignmentId,
            Authentication authentication) {
        return ok(assignmentService.getAssignment(assignmentId, getCurrentUser(authentication).getId()), "Assignment retrieved");
    }

    @PatchMapping("/{assignmentId}")
    @PreAuthorize("hasRole('STAFF')")
    @RequireFeature(FeatureConstants.ASSIGNMENTS)
    @Operation(summary = "Patch a draft assignment")
    public ResponseEntity<ApiResponse<AssignmentDetailResponse>> updateAssignment(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody AssignmentUpdateRequest request,
            Authentication authentication) {
        return ok(assignmentService.updateAssignment(assignmentId, request, getCurrentUser(authentication).getId()), "Assignment updated");
    }

    @PostMapping("/{assignmentId}/publish")
    @PreAuthorize("hasRole('STAFF')")
    @RequireFeature(FeatureConstants.ASSIGNMENTS)
    @Operation(summary = "Publish a draft assignment")
    public ResponseEntity<ApiResponse<AssignmentDetailResponse>> publishAssignment(
            @PathVariable UUID assignmentId,
            Authentication authentication) {
        return ok(assignmentService.publishAssignment(assignmentId, getCurrentUser(authentication).getId()), "Assignment published");
    }

    @PatchMapping("/{assignmentId}/close")
    @PreAuthorize("hasRole('STAFF')")
    @RequireFeature(FeatureConstants.ASSIGNMENTS)
    @Operation(summary = "Close an active assignment")
    public ResponseEntity<ApiResponse<AssignmentDetailResponse>> closeAssignment(
            @PathVariable UUID assignmentId,
            Authentication authentication) {
        return ok(assignmentService.closeAssignment(assignmentId, getCurrentUser(authentication).getId()), "Assignment closed");
    }

    @DeleteMapping("/{assignmentId}")
    @PreAuthorize("hasRole('STAFF')")
    @RequireFeature(FeatureConstants.ASSIGNMENTS)
    @Operation(summary = "Archive an assignment")
    public ResponseEntity<ApiResponse<Void>> archiveAssignment(
            @PathVariable UUID assignmentId,
            Authentication authentication) {
        assignmentService.archiveAssignment(assignmentId, getCurrentUser(authentication).getId());
        return okMessage("Assignment archived");
    }

    @GetMapping("/{assignmentId}/submissions")
    @PreAuthorize("hasRole('STAFF')")
    @RequireFeature(FeatureConstants.ASSIGNMENTS)
    @Operation(summary = "List submissions for an assignment")
    public ResponseEntity<ApiResponse<PageResponse<SubmissionResponse>>> listSubmissions(
            @PathVariable UUID assignmentId,
            @RequestParam(required = false) SubmissionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Pageable pageable = PaginationUtils.of(page, size, "submittedAt");
        return paged(
                assignmentService.listSubmissions(assignmentId, status, getCurrentUser(authentication).getId(), pageable),
                "Submissions retrieved"
        );
    }

    @GetMapping("/submissions/{submissionId}")
    @PreAuthorize("hasRole('STAFF')")
    @RequireFeature(FeatureConstants.ASSIGNMENTS)
    @Operation(summary = "Get submission detail")
    public ResponseEntity<ApiResponse<SubmissionResponse>> getSubmission(
            @PathVariable UUID submissionId,
            Authentication authentication) {
        return ok(assignmentService.getSubmission(submissionId, getCurrentUser(authentication).getId()), "Submission retrieved");
    }

    @PostMapping("/submissions/{submissionId}/grade")
    @PreAuthorize("hasRole('STAFF')")
    @RequireFeature(FeatureConstants.GRADES)
    @Operation(summary = "Grade a student submission")
    public ResponseEntity<ApiResponse<SubmissionResponse>> gradeSubmission(
            @PathVariable UUID submissionId,
            @Valid @RequestBody GradeSubmissionRequest request,
            Authentication authentication) {
        return ok(assignmentService.gradeSubmission(submissionId, request, getCurrentUser(authentication).getId()), "Submission graded");
    }

    @PostMapping("/bulk-grade")
    @PreAuthorize("hasRole('STAFF')")
    @RequireFeature(FeatureConstants.GRADES)
    @Operation(summary = "Bulk grade submissions")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> bulkGrade(
            @Valid @RequestBody BulkGradeRequest request,
            Authentication authentication) {
        return ok(assignmentService.bulkGrade(request, getCurrentUser(authentication).getId()), "Submissions graded");
    }

    @GetMapping("/pending/submissions")
    @PreAuthorize("hasRole('STAFF')")
    @RequireFeature(FeatureConstants.ASSIGNMENTS)
    @Operation(summary = "List all pending submissions across assignments")
    public ResponseEntity<ApiResponse<PageResponse<PendingSubmissionResponse>>> listPendingSubmissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Pageable pageable = PaginationUtils.of(page, size, "submittedAt");
        return paged(
                assignmentService.listPendingSubmissions(getCurrentUser(authentication).getId(), pageable),
                "Pending submissions retrieved"
        );
    }
}
