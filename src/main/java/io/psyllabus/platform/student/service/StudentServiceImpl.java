package io.psyllabus.platform.student.service;


import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.auth.dto.request.RegisterRequest;
import io.psyllabus.platform.auth.dto.responses.UserResponse;
import io.psyllabus.platform.auth.enums.AccessScope;
import io.psyllabus.platform.auth.enums.UserType;
import io.psyllabus.platform.auth.services.AuthService;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.common.exception.DuplicateResourceException;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.student.dto.request.StudentCreateRequest;
import io.psyllabus.platform.student.dto.responses.StudentDetailResponse;
import io.psyllabus.platform.student.dto.responses.StudentResponse;
import io.psyllabus.platform.student.entities.Student;
import io.psyllabus.platform.student.enums.StudentStatus;
import io.psyllabus.platform.student.repository.StudentRepository;
import io.psyllabus.platform.student.service.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class StudentServiceImpl implements StudentService {

    private static final String STUDENT_NOT_FOUND = "Student not found";
    public static final String STUDENT = "STUDENT";

    private final StudentRepository studentRepository;
    private final AuthService authService;
    private final StudentMapper studentMapper;
    private final AuditService auditService;

    @Override
    @CacheEvict(value = "userClaims", key = "#result.userId", condition = "#result != null && #result.userId != null")
    public StudentResponse enrollStudent(StudentCreateRequest request) {
        log.info("Enrolling student" );

        UUID tenantId = TenantContext.getCurrentTenantId();

        // Check for duplicates admission number
        if (studentRepository.existsByAdmissionNumberAndTenantId(
                request.admissionNumber(), tenantId
        )){
            throw new DuplicateResourceException("Admission number already exists");
        }

        // Create user account
        RegisterRequest userRequest = new RegisterRequest(
                request.admissionNumber(), // username = admission number
                resolveUserEmail(request, tenantId),
                request.admissionNumber(), // default password = admission number
                request.firstName(),
                request.lastName(),
                request.phone(),
                UserType.STUDENT,
                request.branchId(),
                AccessScope.BRANCH
        );

        UserResponse userResponse = authService.register(userRequest);

        // Create Student record using mapper
        Student student = studentMapper.toEntity(request);
        student.setTenantId(tenantId);
        student.setUserId(userResponse.id());
        student.setAdmissionDate(LocalDate.now());

        student = studentRepository.save(student);

        log.info("Student enrolled");

        auditService.log(
                AuditAction.ENROLLMENT,
                STUDENT,
                student.getId(),
                "New student enrolled and linked to user account"
        );

        return studentMapper.toResponse(student);
    }

    // TODO: Finish this setup once guardian setup has been established
    @Override
    public void linkGuardian(UUID studentId, UUID guardianId) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        Student student = studentRepository.findByIdAndTenantId(studentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));

        // Todo : Create StudentGuardian junction record
        log.info("Linking student {} to guardian {}", studentId, guardianId);
    }

    @Override
    @CacheEvict(value = "userClaims", key = "#result.userId", condition = "#result != null && #result.userId != null")
    public StudentResponse assignClass(UUID studentId, UUID classId) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        Student student = studentRepository.findByIdAndTenantId(studentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));

        // TODO: Will work when we setup classes
        student.setCurrentClassId(classId);
        student = studentRepository.save(student);

        log.info("Assigned student {} to class {}", studentId, classId);

        auditService.log(
                AuditAction.UPDATE,
                STUDENT,
                studentId,
                String.format("Assigned student to class %s", classId)
        );

        return studentMapper.toResponse(student);
    }

    @Override
    public StudentDetailResponse findByAdmissionNumber(String admissionNumber) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Student student = studentRepository.findByAdmissionNumberAndTenantId(admissionNumber, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));

        return studentMapper.toDetailResponse(student);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> findAll(Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Page<StudentResponse> page = studentRepository
                .findByTenantId(tenantId, pageable)
                .map(studentMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentDetailResponse findById(UUID studentId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Student student = studentRepository.findByIdAndTenantId(studentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));
        return studentMapper.toDetailResponse(student);

    }

    @Override
    @CacheEvict(value = "userClaims", key = "#result.userId", condition = "#result != null && #result.userId != null")
    public StudentResponse updateStatus(UUID studentId, io.psyllabus.platform.student.enums.StudentStatus status) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Student student = studentRepository.findByIdAndTenantId(studentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));

        StudentStatus previous = student.getCurrentStatus();
        student.setCurrentStatus(status);
        
        // Nullify class if no longer enrolled to prevent inflated stats
        if (status == io.psyllabus.platform.student.enums.StudentStatus.TRANSFERRED || 
            status == io.psyllabus.platform.student.enums.StudentStatus.GRADUATED) {
            log.info("Student {} moved to status {}, nulling currentClassId", student.getId(), status);
            student.setCurrentClassId(null);
        }

        Student saved = studentRepository.save(student);

        auditService.log(
                AuditAction.UPDATE,
                STUDENT,
                studentId,
                String.format("Status changed from %s to %s", previous, status)
        );

        return studentMapper.toResponse(saved);
    }

    private String resolveUserEmail(StudentCreateRequest request, UUID tenantId) {
        if (request.email() != null && !request.email().isBlank()) {
            return request.email().trim().toLowerCase(Locale.ROOT);
        }
        return request.admissionNumber().trim().toLowerCase(Locale.ROOT)
                + "+"
                + tenantId
                + "@noemail.psyllabus.internal";
    }
}
