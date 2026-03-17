package io.psyllabus.platform.platformadmin.services;

import io.psyllabus.platform.auth.enums.UserStatus;
import io.psyllabus.platform.common.exception.DuplicateResourceException;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.platformadmin.dto.request.PlatformAdminCreateRequest;
import io.psyllabus.platform.platformadmin.dto.responses.PlatformAdminResponse;
import io.psyllabus.platform.platformadmin.entities.PlatformAdmin;
import io.psyllabus.platform.platformadmin.repositories.PlatformAdminRepository;
import io.psyllabus.platform.school.dto.request.SchoolCreateRequest;
import io.psyllabus.platform.school.dto.responses.SchoolOnboardResponse;
import io.psyllabus.platform.school.dto.responses.SchoolSummaryResponse;
import io.psyllabus.platform.school.entities.School;
import io.psyllabus.platform.school.repository.SchoolRepository;
import io.psyllabus.platform.school.service.SchoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of {@link PlatformAdminService}.
 *
 * <p><strong>Cross-tenant methods in this class are intentional and correct.</strong>
 * {@link #listAllSchools} and {@link #getAnySchool} call {@link SchoolRepository}
 * without reading {@code TenantContext}. This is safe because:
 * {@code PlatformAdminAuthFilter} gates all {@code /api/platform/**} requests,
 * and {@code @PreAuthorize} provides a second check at the controller.</p>
 *
 * <p>Domain strategy validation (subdomain vs custom domain) is handled inside
 * {@code SchoolServiceImpl.onboardSchool()} — that is where school creation lives
 * and where the guard belongs. This class does not duplicate it.</p>
 */

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PlatformAdminServiceImpl implements PlatformAdminService {

    private final SchoolService schoolService;
    private final SchoolRepository schoolRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final PasswordEncoder passwordEncoder;

    // School Provisioning

    @Override
    public SchoolOnboardResponse provisionSchool(SchoolCreateRequest request, UUID adminId) {
        log.info("Platform admin provisioning school: name='{}', plan={}, admin={}",
                request.name(), request.planType(), adminId
        );

        // We Delegate entirely to school service - it owns school creation logic
        // domain strategy validation, and uniqueness checks
        SchoolOnboardResponse base = schoolService.onboardSchool(request);

        // Stamp provisionedBy on the School entity for the audit trail
        // SchoolServiceImpl cannot do this becuase it has no access to the
        // platform admin's identity - this is a platform-layer concern
        School school = schoolRepository.findById(base.schoolId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "School not found after provisioning = this might be a bug in SchoolServiceImpl"
                ));
        school.setProvisionedBy(adminId);
        schoolRepository.save(school);

        log.info("School provisioned: schoolId={}, provisionedBy={}", school.getId(), adminId);

        //Rebuild the response with provisionedByAdmin stamped
        // SchoolServiceImpl returns null for this field since it has no platform context.
        return new SchoolOnboardResponse(
                base.schoolId(),
                base.schoolName(),
                base.subdomainPrefix(),
                base.adminEmail(),
                base.adminTempPassword(),
                base.portalLoginUrl(),
                base.planType(),
                base.provisionedAt(),
                adminId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SchoolSummaryResponse> listAllSchools(Pageable pageable) {
        // Intentionally cross-tenant - no TenantContext read.
        log.debug("Platform admin listing all schools, page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        return schoolRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toSummaryResponse);
    }

    @Transactional
    @Override
    public SchoolSummaryResponse getAnySchool(UUID schoolId) {
        // Intentionally cross Tenant - no tenant context
        log.debug("Platform admin fetching school: schoolId={}", schoolId);

        School school = schoolRepository.findByIdCrossTenant(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found: " + schoolId));
        return toSummaryResponse(school);
    }

    @Override
    public PlatformAdminResponse createPlatformAdmin(PlatformAdminCreateRequest request) {
        log.info("Creating platform admin: username={}", request.username());

        if (platformAdminRepository.existsByUsername(request.username())){
            throw new DuplicateResourceException("Platform admin username already registered: " + request.username());
        }
        if (platformAdminRepository.existsByEmail(request.email())){
            throw new DuplicateResourceException(
                    "Platform admin email already registered: " + request.email()
            );
        }

        PlatformAdmin admin = PlatformAdmin.builder()
                .username(request.username())
                .email(request.email())
                .hashedPassword(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .status(UserStatus.ACTIVE)
                .build();

        PlatformAdmin saved = platformAdminRepository.save(admin);
        log.info("Platform admin created: id={}, username={}", saved.getId(), saved.getUsername());

        return new PlatformAdminResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getEmail(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getStatus(),
                saved.getCreatedAt()
        );
    }

    // Helper methods
    private SchoolSummaryResponse toSummaryResponse(School school) {
        return new SchoolSummaryResponse(
                school.getId(),
                school.getName(),
                school.getEmail(),
                school.getPlan(),
                school.getIsActive(),
                school.getSubdomainPrefix(),
                school.getCustomDomain(),
                school.getCreatedAt(),
                school.getProvisionedBy()
        );
    }
}
