package io.psyllabus.platform.academic.service;

import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.student.dto.responses.StudentResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Manages student-to-class enrollments.
 */
public interface EnrollmentService {

    /**
     * Enrolls a student in a class.
     * Synchronizes the students.current_class_id for consistency.
     */
    void enrollStudent(UUID classId, UUID studentId);

    /**
     * Removes a student from a class (deactivates enrollment).
     */
    void unenrollStudent(UUID classId, UUID studentId);

    /**
     * List all students enrolled in a specific class.
     */
    PageResponse<StudentResponse> listEnrolledStudents(UUID classId, Pageable pageable);

    /**
     * List all classes a specific student is enrolled in.
     */
    PageResponse<io.psyllabus.platform.academic.dto.response.ClassResponse> listStudentEnrollments(UUID studentId, Pageable pageable);
}
