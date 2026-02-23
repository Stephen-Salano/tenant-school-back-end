package io.psyllabus.platform.student.service;

import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.student.dto.request.StudentCreateRequest;
import io.psyllabus.platform.student.dto.responses.StudentDetailResponse;
import io.psyllabus.platform.student.dto.responses.StudentResponse;
import org.springframework.data.domain.Pageable;

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
     */
    void assignClass(UUID studentId, UUID classId);

    /**
     * Find student by admission number
     */
    StudentResponse findByAdmissionNumber(String admissionNumber, UUID tenantId);

    /**
     * Paginated list of all students within the current tenant.
     * Returns StudentResponse (no medical data) — safe for list views.
     */
    PageResponse<StudentResponse> findAll(Pageable pageable);

    /**
     * Full student detail by ID (tenant-scoped).
     * Returns StudentDetailResponse including medicalConditions.
     * Caller (controller) is responsible for RBAC — PARENT must never reach this.
     */
    StudentDetailResponse findById(UUID studentId);
}

