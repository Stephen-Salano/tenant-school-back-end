package io.psyllabus.platform.staff.services;

import io.psyllabus.platform.academic.repository.SubjectGroupRepository;
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
import io.psyllabus.platform.staff.dto.request.StaffCreateRequest;
import io.psyllabus.platform.staff.dto.request.StaffUpdateRequest;
import io.psyllabus.platform.staff.dto.request.UpdateStaffStatusRequest;
import io.psyllabus.platform.staff.dto.responses.StaffDetailResponse;
import io.psyllabus.platform.staff.dto.responses.StaffResponse;
import io.psyllabus.platform.staff.entities.Staff;
import io.psyllabus.platform.staff.enums.EmploymentStatus;
import io.psyllabus.platform.staff.repository.StaffRepository;
import io.psyllabus.platform.staff.service.mapper.StaffMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StaffServiceImpl implements StaffService {

    private static final String STAFF_NOT_FOUND = "Staff not found";
    public static final String SUBJECT_GROUP_NOT_FOUND = "Subject group not found: ";

    private final StaffRepository staffRepository;
    private final AuthService authService;
    private final StaffMapper staffMapper;
    private final SubjectGroupRepository subjectGroupRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final AuditService auditService;
    private final CacheManager cacheManager;

    @Override
    public StaffResponse createStaff(StaffCreateRequest request) {
        log.info("Creating staff: {}", request.employeeNumber());

        UUID tenantId = TenantContext.getCurrentTenantId();

        // Check for duplicate employee number
        if (staffRepository.existsByEmployeeNumberAndTenantId(request.employeeNumber(), tenantId)){
            throw new DuplicateResourceException("Employee number already exists");
        }

        // Check for duplicate email
        if (staffRepository.existsByEmailAndTenantId(request.email(), tenantId)){
            throw new DuplicateResourceException("Employee with that email exists");
        }
        if (request.subjectGroupId() != null) {
            subjectGroupRepository.findByIdAndTenantId(request.subjectGroupId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException(SUBJECT_GROUP_NOT_FOUND + request.subjectGroupId()));
        }
        if (request.branchId() != null) {
            requireBranch(tenantId, request.branchId());
        }

        // create user account
        RegisterRequest userRequest = new RegisterRequest(
                request.employeeNumber(), // username = employee number
                request.email(),
                request.employeeNumber(), // default password = employee number
                request.firstName(),
                request.lastName(),
                request.phone(),
                UserType.STAFF,
                request.branchId(),
                AccessScope.BRANCH
        );

        UserResponse userResponse = authService.register(userRequest);

        // Create Staff record
        Staff staff = Staff.builder()
                .tenantId(tenantId)
                .userId(userResponse.id())
                .employeeNumber(request.employeeNumber())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .subjectGroupId(request.subjectGroupId())
                .hireDate(request.hireDate())
                .qualification(request.qualification())
                .specialization(request.specialization())
                .employmentStatus(EmploymentStatus.ACTIVE)
                .build();

        staff = staffRepository.save(staff);

        log.info("Staff created: {}", staff.getEmployeeNumber());
        return staffMapper.toResponse(staff);
    }

    @Override
    public void assignSubjectGroup(UUID staffId, UUID subjectGroupId) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        Staff staff = staffRepository.findByIdAndTenantId(staffId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(STAFF_NOT_FOUND));

        subjectGroupRepository.findByIdAndTenantId(subjectGroupId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(SUBJECT_GROUP_NOT_FOUND + subjectGroupId));

        staff.setSubjectGroupId(subjectGroupId);
        staffRepository.save(staff);

        log.info("Assigned staff {} to subject group {}", staffId, subjectGroupId);
    }

    @Override
    public StaffResponse findByEmployeeNumber(String employeeNumber, UUID tenantId) {
        Staff staff = staffRepository.findByEmployeeNumberAndTenantId(employeeNumber, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(STAFF_NOT_FOUND));

        return staffMapper.toResponse(staff);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StaffResponse> findAll(Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Page<StaffResponse> page = staffRepository
                .findByTenantId(tenantId, pageable)
                .map(staffMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffDetailResponse findById(UUID staffId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Staff staff = staffRepository.findByIdAndTenantId(staffId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(STAFF_NOT_FOUND));
        return toDetailResponse(staff, tenantId);
    }

    @Override
    public StaffResponse updateStatus(UUID staffId, UpdateStaffStatusRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Staff staff = staffRepository.findByIdAndTenantId(staffId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(STAFF_NOT_FOUND));

        EmploymentStatus previous = staff.getEmploymentStatus();
        staff.setEmploymentStatus(request.status());
        staff = staffRepository.save(staff);
        log.info("Staff {} status: {} -> {} (reason: {})",
                staff.getEmployeeNumber(), previous, request.status(), request.reason());
        return staffMapper.toResponse(staff);
    }

    @Override
    public StaffResponse update(UUID staffId, StaffUpdateRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Staff staff = staffRepository.findByIdAndTenantId(staffId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(STAFF_NOT_FOUND));
        User user = userRepository.findByIdAndTenantId(staff.getUserId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Linked user not found"));

        if (request.firstName() != null) {
            requireText(request.firstName(), "First name");
            staff.setFirstName(request.firstName());
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            requireText(request.lastName(), "Last name");
            staff.setLastName(request.lastName());
            user.setLastName(request.lastName());
        }
        if (request.email() != null) {
            requireText(request.email(), "Email");
            if (staffRepository.existsByEmailAndTenantIdAndIdNot(request.email(), tenantId, staff.getId())
                    || userRepository.existsByEmailAndTenantIdAndIdNot(request.email(), tenantId, user.getId())) {
                throw new DuplicateResourceException("Email already exists");
            }
            staff.setEmail(request.email());
            user.setEmail(request.email());
        }
        if (request.phone() != null) {
            staff.setPhone(request.phone());
            user.setPhone(request.phone());
        }
        if (request.subjectGroupId() != null) {
            subjectGroupRepository.findByIdAndTenantId(request.subjectGroupId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException(SUBJECT_GROUP_NOT_FOUND + request.subjectGroupId()));
            staff.setSubjectGroupId(request.subjectGroupId());
        }
        if (request.hireDate() != null) staff.setHireDate(request.hireDate());
        if (request.employmentStatus() != null) staff.setEmploymentStatus(request.employmentStatus());
        if (request.qualification() != null) staff.setQualification(request.qualification());
        if (request.specialization() != null) staff.setSpecialization(request.specialization());
        if (request.branchId() != null) {
            requireBranch(tenantId, request.branchId());
            user.setBranchId(request.branchId());
        }

        userRepository.save(user);
        Staff saved = staffRepository.save(staff);
        evictUserCaches(user.getId());
        auditService.log(AuditAction.UPDATE, "STAFF", saved.getId(), "Staff profile updated");
        return staffMapper.toResponse(saved);
    }

    @Override
    public void delete(UUID staffId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Staff staff = staffRepository.findByIdAndTenantId(staffId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(STAFF_NOT_FOUND));
        User user = userRepository.findByIdAndTenantId(staff.getUserId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Linked user not found"));

        staff.setEmploymentStatus(EmploymentStatus.TERMINATED);
        user.setStatus(UserStatus.INACTIVE);

        userRepository.save(user);
        staffRepository.save(staff);
        evictUserCaches(user.getId());
        auditService.log(AuditAction.DELETE, "STAFF", staff.getId(), "Staff soft deleted");
    }

    private StaffDetailResponse toDetailResponse(Staff staff, UUID tenantId) {
        String subjectGroupName = staff.getSubjectGroupId() == null
                ? null
                : subjectGroupRepository.findNameByIdAndTenantId(staff.getSubjectGroupId(), tenantId).orElse(null);
        return new StaffDetailResponse(
                staff.getId(),
                staff.getEmployeeNumber(),
                staff.getFirstName(),
                staff.getLastName(),
                staff.getEmail(),
                staff.getPhone(),
                staff.getSubjectGroupId(),
                subjectGroupName,
                staff.getHireDate(),
                staff.getEmploymentStatus(),
                staff.getQualification(),
                staff.getSpecialization()
        );
    }

    private void requireBranch(UUID tenantId, UUID branchId) {
        if (!branchRepository.existsByIdAndTenantId(branchId, tenantId)) {
            throw new ResourceNotFoundException("Branch not found: " + branchId);
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
