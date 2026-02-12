package io.psyllabus.platform.student.service;

import io.psyllabus.platform.student.dto.request.StudentCreateRequest;
import io.psyllabus.platform.student.dto.responses.StudentResponse;

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
}

