package io.psyllabus.platform.guardian.services;

import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.service.AuditService;
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
import io.psyllabus.platform.guardian.service.mapper.GuardianMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
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
public class GuardianServiceImpl implements GuardianService {

    private final GuardianRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final AuthService authService;
    private final AuditService auditService;
    private final GuardianMapper guardianMapper;

    @Value("${platform.default-passwords.guardian:Parent@123}")
    private String guardianTempPassword;

    /*
     TODO: private final UUID tenantId = TenantContext.getCurrentTenantId(); globally instead of calling it every time
     */

    @Override
    @CacheEvict(value = "userClaims", key = "#result.userId", condition = "#result != null && #result.userId != null")
    public GuardianResponse createGuardian(GuardianCreateRequest request) {
        log.info("Creating new guardian record");

        UUID tenantId = TenantContext.getCurrentTenantId();

        // check for duplicate email
        if (request.email() != null && !request.email().isBlank() && guardianRepository.existsByEmailAndTenantId(request.email(), tenantId)) {
            throw new DuplicateResourceException(" Email already exists");
        }
        // Check for duplicate phone
        if (guardianRepository.existsByPhoneAndTenantId(request.phone(), tenantId)) {
            throw new DuplicateResourceException("Phone number already exists");
        }

        RegisterRequest userRequest = getRegisterRequest(request);

        UserResponse userResponse = authService.register(userRequest);

        // Create Guardian record using mapper
        Guardian guardian = guardianMapper.toEntity(request);
        guardian.setTenantId(tenantId);
        guardian.setUserId(userResponse.id());
        
        if (guardian.getIsFeepayer() == null) {
            guardian.setIsFeepayer(true);
        }

        guardian = guardianRepository.save(guardian);
        log.info("Guardian created");

        auditService.log(
                AuditAction.CREATE,
                "GUARDIAN",
                guardian.getId(),
                "New guardian created and linked to user account"
        );

        return guardianMapper.toResponse(guardian);
    }


    private @NonNull RegisterRequest getRegisterRequest(GuardianCreateRequest request) {
        boolean hasEmail = request.email() != null && !request.email().isBlank();

        // Use email as username if available; otherwise, use phone with a prefix
        // Prefixing helps avoid collisions and clearly identifies the source
        String username = hasEmail ? request.email() : "G-" + request.phone();

        // Auth services often have NOT NULL constraints on email.
        // If no email exists, provide a consistent internal placeholder.
        String userEmail = hasEmail ? request.email() : (request.phone() + "@noemail.psyllabus.internal");

        return new RegisterRequest(
                username,
                userEmail,
                guardianTempPassword,
                request.firstName(),
                request.lastName(),
                request.phone(),
                UserType.PARENT,
                null,
                AccessScope.SCHOOL
        );
    }

    @Override
    @CacheEvict(value = "userClaims", key = "#result.userId", condition = "#result != null && #result.userId != null")
    public GuardianResponse linkToStudent(UUID guardianId, UUID studentId) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        // Verify that the guardian does exist
        Guardian guardian = guardianRepository.findByIdAndTenantId(guardianId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Guardian not found"));

        // Check if link already exists
        if (studentGuardianRepository.existsByStudentIdAndGuardianIdAndTenantId(
                studentId, guardianId, tenantId
        )) {
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

        log.info("Link guardian {} to student {}", guardianId, studentId);

        auditService.log(
                AuditAction.UPDATE,
                "GUARDIAN_RELATIONSHIP",
                guardianId,
                String.format("Linked guardian to student %s", studentId)
        );

        return guardianMapper.toResponse(guardian);
    }


    @Override
    @CacheEvict(value = "userClaims", key = "#result.userId", condition = "#result != null && #result.userId != null")
    public GuardianResponse unlinkFromStudent(UUID guardianId, UUID studentId) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        Guardian guardian = guardianRepository.findByIdAndTenantId(guardianId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Guardian not found"));

        studentGuardianRepository.deleteByStudentIdAndGuardianIdAndTenantId(
                studentId, guardianId, tenantId
        );

        log.info(" Unlinked guardian {} from student {}", guardianId, studentId);

        auditService.log(
                AuditAction.UPDATE,
                "GUARDIAN_RELATIONSHIP",
                guardianId,
                String.format("Unlinked guardian from student %s", studentId)
        );

        return guardianMapper.toResponse(guardian);
    }


    @Override
    @Transactional(readOnly = true)
    public GuardianResponse findByEmail(String email, UUID tenantId) {
        Guardian guardian = guardianRepository.findByEmailAndTenantId(email, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(" Guardian not found"));
        return guardianMapper.toResponse(guardian);
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
                .map(guardianMapper::toResponse)
                .toList();
    }

}
