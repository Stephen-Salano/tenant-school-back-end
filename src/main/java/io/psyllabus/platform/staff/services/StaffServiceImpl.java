package io.psyllabus.platform.staff.services;

import io.psyllabus.platform.auth.dto.request.RegisterRequest;
import io.psyllabus.platform.auth.dto.responses.UserResponse;
import io.psyllabus.platform.auth.enums.AccessScope;
import io.psyllabus.platform.auth.enums.UserType;
import io.psyllabus.platform.auth.services.AuthService;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.common.exception.DuplicateResourceException;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.staff.dto.request.StaffCreateRequest;
import io.psyllabus.platform.staff.dto.request.UpdateStaffStatusRequest;
import io.psyllabus.platform.staff.dto.responses.StaffDetailResponse;
import io.psyllabus.platform.staff.dto.responses.StaffResponse;
import io.psyllabus.platform.staff.entities.Staff;
import io.psyllabus.platform.staff.enums.EmploymentStatus;
import io.psyllabus.platform.staff.repository.StaffRepository;
import io.psyllabus.platform.staff.service.mapper.StaffMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StaffServiceImpl implements StaffService {

    private static final String STAFF_NOT_FOUND = "Staff not found";

    private final StaffRepository staffRepository;
    private final AuthService authService;
    private final StaffMapper staffMapper;

    @Override
    @CacheEvict(value = "userClaims", key = "#result.userId")
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
    @CacheEvict(value = "userClaims", key = "#result.userId")
    public StaffResponse assignSubjectGroup(UUID staffId, UUID subjectGroupId) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        Staff staff = staffRepository.findByIdAndTenantId(staffId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(STAFF_NOT_FOUND));

        staff.setSubjectGroupId(subjectGroupId);
        staff = staffRepository.save(staff);

        log.info("Assigned staff {} to subject group {}", staffId, subjectGroupId);
        return staffMapper.toResponse(staff);
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
        return staffMapper.toDetailResponse(staff);
    }

    @Override
    @CacheEvict(value = "userClaims", key = "#result.userId")
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
}
