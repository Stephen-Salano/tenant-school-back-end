package io.psyllabus.platform.student.service;


import io.psyllabus.platform.academic.repository.SchoolClassRepository;
import io.psyllabus.platform.auth.dto.request.RegisterRequest;
import io.psyllabus.platform.auth.dto.responses.UserResponse;
import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.auth.enums.AccessScope;
import io.psyllabus.platform.auth.enums.UserStatus;
import io.psyllabus.platform.auth.enums.UserType;
import io.psyllabus.platform.auth.repository.UserRepository;
import io.psyllabus.platform.auth.services.AuthService;
import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.branch.repository.BranchRepository;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.common.exception.DuplicateResourceException;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.exception.ValidationException;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.student.dto.request.StudentCreateRequest;
import io.psyllabus.platform.student.dto.request.StudentUpdateRequest;
import io.psyllabus.platform.student.dto.responses.StudentDetailResponse;
import io.psyllabus.platform.student.dto.responses.StudentResponse;
import io.psyllabus.platform.student.entities.Student;
import io.psyllabus.platform.student.enums.StudentStatus;
import io.psyllabus.platform.student.repository.StudentRepository;
import io.psyllabus.platform.student.service.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class StudentServiceImpl implements StudentService {

    private static final String STUDENT_NOT_FOUND = "Student not found";
    public static final String CLASS_NOT_FOUND = "Class not found: ";

    private final StudentRepository studentRepository;
    private final AuthService authService;
    private final StudentMapper studentMapper;
    private final SchoolClassRepository schoolClassRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final AuditService auditService;
    private final CacheManager cacheManager;

    @Override
    @CacheEvict(value = "userClaims", key = "#result.userId")
    public StudentResponse enrollStudent(StudentCreateRequest request) {
        log.info("Enrolling student" );

        UUID tenantId = TenantContext.getCurrentTenantId();

        // Check for duplicates admission number
        if (studentRepository.existsByAdmissionNumberAndTenantId(
                request.admissionNumber(), tenantId
        )){
            throw new DuplicateResourceException("Admission number already exists");
        }
        requireClassIfPresent(tenantId, request.classId());
        if (request.branchId() != null) {
            requireBranch(tenantId, request.branchId());
        }

        // Create user account
        RegisterRequest userRequest = new RegisterRequest(
                request.admissionNumber(), // username = admission number
                request.email(),
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

        log.info("Student enrolled: {}", student.getAdmissionNumber());

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
    @CacheEvict(value = "userClaims", key = "#result.userId")
    public StudentResponse assignClass(UUID studentId, UUID classId) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        Student student = studentRepository.findByIdAndTenantId(studentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));

        schoolClassRepository.findByIdAndTenantId(classId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(CLASS_NOT_FOUND + classId));

        student.setCurrentClassId(classId);
        student = studentRepository.save(student);

        log.info("Assigned student {} to class {}", studentId, classId);
        return studentMapper.toResponse(student);
    }

    @Override
    public StudentDetailResponse findByAdmissionNumber(String admissionNumber) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Student student = studentRepository.findByAdmissionNumberAndTenantId(admissionNumber, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));

        return toDetailResponse(student, tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> findAll(UUID classId, Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Page<StudentResponse> page;
        if (classId == null) {
            page = studentRepository.findByTenantId(tenantId, pageable)
                    .map(studentMapper::toResponse);
        } else {
            schoolClassRepository.findByIdAndTenantId(classId, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException(CLASS_NOT_FOUND + classId));
            page = studentRepository
                    .findByCurrentClassIdAndTenantIdAndCurrentStatus(classId, tenantId, StudentStatus.ACTIVE, pageable)
                    .map(studentMapper::toResponse);
        }
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentDetailResponse findById(UUID studentId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Student student = studentRepository.findByIdAndTenantId(studentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));
        return toDetailResponse(student, tenantId);

    }

    @Override
    @CacheEvict(value = "userClaims", key = "#result.userId")
    public StudentResponse updateStatus(UUID studentId, io.psyllabus.platform.student.enums.StudentStatus status) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Student student = studentRepository.findByIdAndTenantId(studentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));

        student.setCurrentStatus(status);
        
        // Nullify class if no longer enrolled to prevent inflated stats
        if (status == io.psyllabus.platform.student.enums.StudentStatus.TRANSFERRED || 
            status == io.psyllabus.platform.student.enums.StudentStatus.GRADUATED) {
            log.info("Student {} moved to status {}, nulling currentClassId", student.getAdmissionNumber(), status);
            student.setCurrentClassId(null);
        }

        Student saved = studentRepository.save(student);
        return studentMapper.toResponse(saved);
    }

    @Override
    public StudentResponse update(UUID studentId, StudentUpdateRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Student student = studentRepository.findByIdAndTenantId(studentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));
        User user = userRepository.findByIdAndTenantId(student.getUserId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Linked user not found"));

        if (request.firstName() != null) {
            requireText(request.firstName(), "First name");
            student.setFirstName(request.firstName());
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            requireText(request.lastName(), "Last name");
            student.setLastName(request.lastName());
            user.setLastName(request.lastName());
        }
        if (request.dateOfBirth() != null) student.setDateOfBirth(request.dateOfBirth());
        if (request.gender() != null) student.setGender(request.gender());
        if (request.classId() != null) {
            requireClassIfPresent(tenantId, request.classId());
            student.setCurrentClassId(request.classId());
        }
        if (request.status() != null) {
            student.setCurrentStatus(request.status());
            if (request.status() != StudentStatus.ACTIVE) {
                student.setCurrentClassId(null);
            }
        }
        if (request.medicalConditions() != null) student.setMedicalConditions(request.medicalConditions());
        if (request.email() != null) {
            requireText(request.email(), "Email");
            if (userRepository.existsByEmailAndTenantIdAndIdNot(request.email(), tenantId, user.getId())) {
                throw new DuplicateResourceException("Email already exists");
            }
            user.setEmail(request.email());
        }
        if (request.phone() != null) user.setPhone(request.phone());
        if (request.branchId() != null) {
            requireBranch(tenantId, request.branchId());
            user.setBranchId(request.branchId());
        }

        userRepository.save(user);
        Student saved = studentRepository.save(student);
        evictUserCaches(user.getId());
        auditService.log(AuditAction.UPDATE, "STUDENT", saved.getId(), "Student profile updated");
        return studentMapper.toResponse(saved);
    }

    @Override
    public void delete(UUID studentId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Student student = studentRepository.findByIdAndTenantId(studentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));
        User user = userRepository.findByIdAndTenantId(student.getUserId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Linked user not found"));

        student.setCurrentStatus(StudentStatus.INACTIVE);
        student.setCurrentClassId(null);
        user.setStatus(UserStatus.INACTIVE);

        userRepository.save(user);
        studentRepository.save(student);
        evictUserCaches(user.getId());
        auditService.log(AuditAction.DELETE, "STUDENT", student.getId(), "Student soft deleted");
    }

    private StudentDetailResponse toDetailResponse(Student student, UUID tenantId) {
        String currentClassName = student.getCurrentClassId() == null
                ? null
                : schoolClassRepository.findNameByIdAndTenantId(student.getCurrentClassId(), tenantId).orElse(null);
        return new StudentDetailResponse(
                student.getId(),
                student.getAdmissionNumber(),
                student.getFirstName(),
                student.getLastName(),
                student.getDateOfBirth(),
                student.getGender(),
                student.getCurrentClassId(),
                currentClassName,
                student.getAdmissionDate(),
                student.getCurrentStatus(),
                student.getMedicalConditions()
        );
    }

    private void requireBranch(UUID tenantId, UUID branchId) {
        if (!branchRepository.existsByIdAndTenantId(branchId, tenantId)) {
            throw new ResourceNotFoundException("Branch not found: " + branchId);
        }
    }

    private void requireClassIfPresent(UUID tenantId, UUID classId) {
        if (classId != null) {
            schoolClassRepository.findByIdAndTenantId(classId, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException(CLASS_NOT_FOUND + classId));
        }
    }

    private void requireText(String value, String fieldName) {
        if (value.isBlank()) {
            throw new ValidationException(fieldName + " cannot be blank");
        }
    }

    private void evictUserCaches(UUID userId) {
        Objects.requireNonNull(cacheManager.getCache("userProfile")).evict(userId);
        Objects.requireNonNull(cacheManager.getCache("userClaims")).evict(userId);
    }
}
