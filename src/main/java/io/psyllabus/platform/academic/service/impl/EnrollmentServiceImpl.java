package io.psyllabus.platform.academic.service.impl;

import io.psyllabus.platform.academic.dto.response.ClassResponse;
import io.psyllabus.platform.academic.entities.SchoolClass;
import io.psyllabus.platform.academic.entities.StudentClassEnrollment;
import io.psyllabus.platform.academic.repository.SchoolClassRepository;
import io.psyllabus.platform.academic.repository.StudentClassEnrollmentRepository;
import io.psyllabus.platform.academic.service.EnrollmentService;
import io.psyllabus.platform.academic.service.mapper.ClassMapper;
import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.common.exception.DuplicateResourceException;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.student.dto.responses.StudentResponse;
import io.psyllabus.platform.student.entities.Student;
import io.psyllabus.platform.student.repository.StudentRepository;
import io.psyllabus.platform.student.service.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EnrollmentServiceImpl implements EnrollmentService {

    private final StudentClassEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository classRepository;
    private final StudentMapper studentMapper;
    private final ClassMapper classMapper;
    private final AuditService auditService;

    @Override
    public void enrollStudent(UUID classId, UUID studentId) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        Student student = studentRepository.findByIdAndTenantId(studentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        SchoolClass schoolClass = classRepository.findByIdAndTenantId(classId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));

        if (enrollmentRepository.existsByStudentIdAndClassIdAndTenantIdAndIsActiveTrue(studentId, classId, tenantId)) {
            throw new DuplicateResourceException("Student is already enrolled in this class");
        }

        StudentClassEnrollment enrollment = enrollmentRepository.findByStudentIdAndClassIdAndTenantId(studentId, classId, tenantId)
                .map(existing -> {
                    existing.setAcademicYearId(schoolClass.getAcademicYearId());
                    existing.setActive(true);
                    return existing;
                })
                .orElseGet(() -> StudentClassEnrollment.builder()
                        .tenantId(tenantId)
                        .studentId(studentId)
                        .classId(classId)
                        .academicYearId(schoolClass.getAcademicYearId())
                        .isActive(true)
                        .build());

        enrollmentRepository.save(enrollment);

        student.setCurrentClassId(classId);
        studentRepository.save(student);

        log.info("Student {} enrolled in class {} [tenant={}]", studentId, classId, tenantId);
        auditService.log(AuditAction.ENROLLMENT, "STUDENT_CLASS_ENROLLMENT", enrollment.getId(), 
                String.format("Enrolled student %s in class %s", studentId, classId));
    }

    @Override
    public void unenrollStudent(UUID classId, UUID studentId) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        StudentClassEnrollment enrollment = enrollmentRepository.findByStudentIdAndClassIdAndTenantId(studentId, classId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment record not found"));

        enrollment.setActive(false);
        enrollmentRepository.save(enrollment);

        // Clear currentClassId if this was their active class
        Student student = studentRepository.findByIdAndTenantId(studentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        
        if (classId.equals(student.getCurrentClassId())) {
            student.setCurrentClassId(null);
            studentRepository.save(student);
        }

        log.info("Student {} unenrolled from class {} [tenant={}]", studentId, classId, tenantId);
        auditService.log(AuditAction.DELETE, "STUDENT_CLASS_ENROLLMENT", enrollment.getId(), 
                String.format("Unenrolled student %s from class %s", studentId, classId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> listEnrolledStudents(UUID classId, Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        classRepository.findByIdAndTenantId(classId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        Page<Student> studentPage = studentRepository.findAllEnrolledByClassIdAndTenantId(classId, tenantId, pageable);
        return PageResponse.from(studentPage.map(studentMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClassResponse> listStudentEnrollments(UUID studentId, Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        Page<StudentClassEnrollment> enrollments = enrollmentRepository.findAllByStudentIdAndTenantIdAndIsActiveTrue(studentId, tenantId, pageable);
        
        List<UUID> classIds = enrollments.getContent().stream()
                .map(StudentClassEnrollment::getClassId)
                .toList();

        List<SchoolClass> classes = classIds.isEmpty()
                ? List.of()
                : classRepository.findAllByIdInAndTenantId(classIds, tenantId);

        return new PageResponse<>(
                classes.stream().map(classMapper::toResponse).toList(),
                enrollments.getNumber(),
                enrollments.getSize(),
                enrollments.getTotalElements(),
                enrollments.getTotalPages(),
                enrollments.isLast()
        );
    }
}
