package io.psyllabus.platform.assignment.service;

import io.psyllabus.platform.assignment.dto.request.AssignmentSubmitRequest;
import io.psyllabus.platform.assignment.dto.response.StudentAssignmentDetailResponse;
import io.psyllabus.platform.assignment.dto.response.StudentAssignmentResponse;
import io.psyllabus.platform.assignment.enums.SubmissionStatus;
import io.psyllabus.platform.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface StudentAssignmentService {

    PageResponse<StudentAssignmentResponse> listAssignments(UUID currentUserId, SubmissionStatus status, Pageable pageable);

    StudentAssignmentDetailResponse getAssignment(UUID assignmentId, UUID currentUserId);

    StudentAssignmentDetailResponse submitAssignment(UUID assignmentId, AssignmentSubmitRequest request, UUID currentUserId);
}
