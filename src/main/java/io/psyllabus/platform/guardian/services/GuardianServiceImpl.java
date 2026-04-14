package io.psyllabus.platform.guardian.services;

import io.psyllabus.platform.auth.dto.request.RegisterRequest;
import io.psyllabus.platform.auth.dto.responses.UserResponse;
import io.psyllabus.platform.auth.enums.AccessScope;
import io.psyllabus.platform.auth.enums.UserType;
import io.psyllabus.platform.auth.services.AuthService;
import io.psyllabus.platform.common.exception.DuplicateResourceException;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.guardian.dto.request.GuardianCreateRequest;
import io.psyllabus.platform.guardian.dto.responses.GuardianResponse;
import io.psyllabus.platform.guardian.entities.Guardian;
import io.psyllabus.platform.guardian.entities.StudentGuardian;
import io.psyllabus.platform.guardian.repository.GuardianRepository;
import io.psyllabus.platform.guardian.repository.StudentGuardianRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GuardianServiceImpl implements  GuardianService{

    private final GuardianRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final AuthService authService;

    @Value("${platform.default-passwords.guardian:Parent@123}")
    private String guardianTempPassword;

    /*
     TODO: private final UUID tenantId = TenantContext.getCurrentTenantId(); globally instead of calling it every time
     */

    @Override
    @CacheEvict(value = "userClaims", key = "#result.userId")
    public GuardianResponse createGuardian(GuardianCreateRequest request) {
        log.info("Creating new guardian record");

        UUID tenantId = TenantContext.getCurrentTenantId();

        // check for duplicate email
        if (guardianRepository.existsByEmailAndTenantId(request.email(), tenantId)){
            throw new DuplicateResourceException(" Email already exists");
        }
        // Check for duplicate phone
        if (guardianRepository.existsByPhoneAndTenantId(request.phone(), tenantId)){
            throw new DuplicateResourceException("Phone number already exists");
        }

        // Create user account
        RegisterRequest userRequest = new RegisterRequest(
            request.email(), // Username  = email
                request.email(),
                guardianTempPassword, // default password = phone number
                request.firstName(),
                request.lastName(),
                request.phone(),
                UserType.PARENT,
                null, // guardians don't have a branch scope
                AccessScope.SCHOOL
        );

        UserResponse userResponse = authService.register(userRequest);

        // Create Guardian record
        Guardian guardian = Guardian.builder()
                .tenantId(tenantId)
                .userId(userResponse.id())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .email(request.email())
                .relationshipType(request.relationshipType())
                .isFeepayer(request.isFeePayer() != null ? request.isFeePayer() : true)
                .build();

        guardian = guardianRepository.save(guardian);
        log.info("Guardian created: {}", guardian.getEmail());
        return mapToResponse(guardian);
    }

    @Override
    @CacheEvict(value = "userClaims", key = "#result.userId")
    public GuardianResponse linkToStudent(UUID guardianId, UUID studentId) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        // Verify that the guardian does exist
        Guardian guardian = guardianRepository.findByIdAndTenantId(guardianId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Guardian not found"));

        // Check if link already exists
        if (studentGuardianRepository.existsByStudentIdAndGuardianIdAndTenantId(
                studentId, guardianId, tenantId
        )){
            throw new DuplicateResourceException("Guardian already linked to this student");
        }

        // Create Link
        StudentGuardian link = StudentGuardian.builder()
                .tenantId(tenantId)
                .studentId(studentId)
                .guardianId(guardianId)
                .relationshipType(guardian.getRelationshipType())
                .isPrimaryContact(false)
                .isEmergencyContact(false)
                .build();

        studentGuardianRepository.save(link);

        log.info("Link guardian {} to student {}", guardianId , studentId);
        return mapToResponse(guardian);
    }

    @Override
    @CacheEvict(value = "userClaims", key = "#result.userId")
    public GuardianResponse unlinkFromStudent(UUID guardianId, UUID studentId) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        Guardian guardian = guardianRepository.findByIdAndTenantId(guardianId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Guardian not found"));

        studentGuardianRepository.deleteByStudentIdAndGuardianIdAndTenantId(
                studentId, guardianId, tenantId
        );

        log.info(" Unlinked guardian {} from student {}", guardianId, studentId);
        return mapToResponse(guardian);
    }

    @Override
    @Transactional(readOnly = true)
    public GuardianResponse findByEmail(String email, UUID tenantId) {
        Guardian guardian = guardianRepository.findByEmailAndTenantId(email, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(" Guardian not found"));
        return mapToResponse(guardian);
    }

    @Override
    public List<GuardianResponse> findByStudentId(UUID studentId) {

        UUID tenantId = TenantContext.getCurrentTenantId();

        // Get all guardian IDs linked to this student
        List<UUID> guardianIds = studentGuardianRepository
                .findByStudentIdAndTenantId(studentId, tenantId)
                .stream()
                .map(StudentGuardian::getGuardianId)
                .toList();

        if (guardianIds.isEmpty()) return List.of();

        // Batch fetch guardians to avoid N+1
        return guardianRepository.findByIdInAndTenantId(guardianIds, tenantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private GuardianResponse mapToResponse(Guardian guardian) {
        return new GuardianResponse(
                guardian.getId(),
                guardian.getUserId(),
                guardian.getFirstName(),
                guardian.getLastName(),
                guardian.getPhone(),
                guardian.getEmail(),
                guardian.getRelationshipType()
        );
    }
}
