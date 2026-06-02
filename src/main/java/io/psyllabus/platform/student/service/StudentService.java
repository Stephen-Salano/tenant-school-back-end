package io.psyllabus.platform.student.service;

import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.student.dto.request.StudentCreateRequest;
import io.psyllabus.platform.student.dto.request.StudentUpdateRequest;
import io.psyllabus.platform.student.dto.responses.StudentDetailResponse;
import io.psyllabus.platform.student.dto.responses.StudentResponse;
import io.psyllabus.platform.student.dto.responses.StudentSubjectResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface StudentService {

    /**
     * Enroll a new student (creates User + Student)
     */
    StudentResponse enrollStudent(StudentCreateRequest request);

    /**
     * Link guardian to student
     */
    void linkGuardian(UUID studentId, UUID guardianId);

    /**
     * Assign student to class
     * @return the updated student response
     */
    StudentResponse assignClass(UUID studentId, UUID classId);

    /**
     * Find student by admission number
     */
    StudentDetailResponse findByAdmissionNumber(String admissionNumber);

    /**
     * Paginated list of students within the current tenant, optionally filtered by class.
     * Returns StudentResponse (no medical data) — safe for list views.
     */
    PageResponse<StudentResponse> findAll(UUID classId, Pageable pageable);

    /**
     * Full student detail by ID (tenant-scoped).
     * Returns StudentDetailResponse including medicalConditions.
     * Caller (controller) is responsible for RBAC — PARENT must never reach this.
     */
    StudentDetailResponse findById(UUID studentId);

    /**
     * Update student profile info (Partial update).
     */
    StudentResponse updateStudent(UUID studentId, StudentUpdateRequest request);

    /**
     * Soft delete student (sets status to INACTIVE).
     */
    void deleteStudent(UUID studentId);

    /**
     * Update student status (e.g. ACTIVE -> GRADUATED).
     * If status is TRANSFERRED or GRADUATED, currentClassId is nulled.
     */
    StudentResponse updateStatus(UUID studentId, io.psyllabus.platform.student.enums.StudentStatus status);

    /**
     * Get subjects student is enrolled in for current term.
     */
    List<StudentSubjectResponse> getStudentSubjects(UUID studentId);
}

