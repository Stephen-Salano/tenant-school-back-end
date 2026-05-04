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

        // 1. Verify existence and tenant ownership
        Student student = studentRepository.findByIdAndTenantId(studentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        SchoolClass schoolClass = classRepository.findByIdAndTenantId(classId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));

        // 2. Check if already enrolled
        if (enrollmentRepository.existsByStudentIdAndClassIdAndTenantId(studentId, classId, tenantId)) {
            throw new DuplicateResourceException("Student is already enrolled in this class");
        }

        // 3. Create enrollment
        StudentClassEnrollment enrollment = StudentClassEnrollment.builder()
                .tenantId(tenantId)
                .studentId(studentId)
                .classId(classId)
                .academicYearId(schoolClass.getAcademicYearId())
                .isActive(true)
                .build();

        enrollmentRepository.save(enrollment);

        // 4. Synchronize student.currentClassId for convenience
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

        // Check if class exists
        if (!classRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Class not found");
        }

        List<UUID> studentIds = enrollmentRepository.findAllByClassIdAndTenantIdAndIsActiveTrue(classId, tenantId)
                .stream()
                .map(StudentClassEnrollment::getStudentId)
                .toList();

        // Note: this approach loses pagination on the join if not careful.
        // For Phase 0 MVP, we return the full list if pageable is default, 
        // or we need a proper query in studentRepository.
        
        // Better: Query studentRepository directly
        Page<Student> studentPage = studentRepository.findByTenantId(tenantId, pageable); // This is NOT filtered by class
        
        // Correct way for Phase 0:
        List<Student> students = studentRepository.findAllByCurrentClassIdAndTenantId(classId, tenantId);
        // Map to response
        List<StudentResponse> responses = students.stream().map(studentMapper::toResponse).toList();
        
        // Re-wrap in PageResponse (even if single page for now)
        return new PageResponse<>(
                responses,
                0, responses.size(), responses.size(), 1, true
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClassResponse> listStudentEnrollments(UUID studentId, Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        Page<StudentClassEnrollment> enrollments = enrollmentRepository.findAllByStudentIdAndTenantIdAndIsActiveTrue(studentId, tenantId, pageable);
        
        List<UUID> classIds = enrollments.getContent().stream()
                .map(StudentClassEnrollment::getClassId)
                .toList();

        List<SchoolClass> classes = classRepository.findAllById(classIds); // Basic fetch

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
