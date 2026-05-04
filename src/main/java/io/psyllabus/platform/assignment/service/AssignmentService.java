package io.psyllabus.platform.assignment.service;

import io.psyllabus.platform.assignment.dto.request.AssignmentCreateRequest;
import io.psyllabus.platform.assignment.dto.request.AssignmentUpdateRequest;
import io.psyllabus.platform.assignment.dto.request.GradeSubmissionRequest;
import io.psyllabus.platform.assignment.dto.response.AssignmentDetailResponse;
import io.psyllabus.platform.assignment.dto.response.AssignmentResponse;
import io.psyllabus.platform.assignment.dto.response.SubmissionResponse;
import io.psyllabus.platform.assignment.enums.AssignmentStatus;
import io.psyllabus.platform.assignment.enums.SubmissionStatus;
import io.psyllabus.platform.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AssignmentService {

    AssignmentDetailResponse createAssignment(AssignmentCreateRequest request, UUID currentUserId);

    PageResponse<AssignmentResponse> listAssignments(UUID currentUserId, UUID classId, AssignmentStatus status, Pageable pageable);

    AssignmentDetailResponse getAssignment(UUID assignmentId, UUID currentUserId);

    AssignmentDetailResponse updateAssignment(UUID assignmentId, AssignmentUpdateRequest request, UUID currentUserId);

    AssignmentDetailResponse publishAssignment(UUID assignmentId, UUID currentUserId);

    AssignmentDetailResponse closeAssignment(UUID assignmentId, UUID currentUserId);

    void archiveAssignment(UUID assignmentId, UUID currentUserId);

    PageResponse<SubmissionResponse> listSubmissions(UUID assignmentId, SubmissionStatus status, UUID currentUserId, Pageable pageable);

    SubmissionResponse getSubmission(UUID submissionId, UUID currentUserId);

    SubmissionResponse gradeSubmission(UUID submissionId, GradeSubmissionRequest request, UUID currentUserId);

    void createPendingSubmissions(UUID assignmentId);
}
