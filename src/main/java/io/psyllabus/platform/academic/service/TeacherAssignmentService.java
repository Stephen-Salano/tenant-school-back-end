package io.psyllabus.platform.academic.service;

import io.psyllabus.platform.academic.dto.request.TeacherAssignmentCreateRequest;
import io.psyllabus.platform.academic.dto.response.TeacherAssignmentResponse;
import java.util.List;
import java.util.UUID;

/**
 * Manages teacher assignments to classes and subjects.
 */
public interface TeacherAssignmentService {
    /**
     * Assign a teacher to a class, subject, and term.
     * @param request assignment details
     * @return the created assignment
     */
    TeacherAssignmentResponse assignTeacher(TeacherAssignmentCreateRequest request);

    /**
     * Fetch all assignments for a specific class.
     * @param classId the class UUID
     * @return list of assignments
     */
    List<TeacherAssignmentResponse> getAssignmentsByClass(UUID classId);

    /**
     * Fetch all assignments for a specific staff member.
     * @param staffId the staff member UUID
     * @return list of assignments
     */
    List<TeacherAssignmentResponse> getAssignmentsByStaff(UUID staffId);

    /**
     * Fetch all assignments for the currently authenticated user (as staff).
     * @param userId the user UUID
     * @return list of assignments
     */
    List<TeacherAssignmentResponse> getAssignmentsByUser(UUID userId);
}
