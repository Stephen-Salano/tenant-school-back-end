package io.psyllabus.platform.platformadmin.services;

import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.auth.enums.UserStatus;
import io.psyllabus.platform.common.exception.DuplicateResourceException;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.onboarding.entity.ImportJob;
import io.psyllabus.platform.onboarding.enums.ImportJobStatus;
import io.psyllabus.platform.onboarding.enums.ImportJobType;
import io.psyllabus.platform.onboarding.repository.ImportJobRepository;
import io.psyllabus.platform.platformadmin.dto.request.PlatformAdminCreateRequest;
import io.psyllabus.platform.platformadmin.dto.responses.PlatformAdminResponse;
import io.psyllabus.platform.platformadmin.dto.responses.PlatformImportJobResponse;
import io.psyllabus.platform.platformadmin.dto.responses.PlatformKpiResponse;
import io.psyllabus.platform.platformadmin.entities.PlatformAdmin;
import io.psyllabus.platform.platformadmin.repositories.PlatformAdminRepository;
import io.psyllabus.platform.school.dto.request.SchoolCreateRequest;
import io.psyllabus.platform.school.dto.responses.SchoolOnboardResponse;
import io.psyllabus.platform.school.dto.responses.SchoolSummaryResponse;
import io.psyllabus.platform.school.entities.School;
import io.psyllabus.platform.school.enums.SchoolStatus;
import io.psyllabus.platform.school.repository.SchoolRepository;
import io.psyllabus.platform.school.service.SchoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
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
    private final ImportJobRepository importJobRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

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
        School school = schoolRepository.findByIdCrossTenant(base.schoolId())
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
    @Transactional(readOnly = true)
    public PlatformKpiResponse getDashboardKpis() {
        long totalSchools = schoolRepository.count();
        long activeSchools = schoolRepository.countByStatus(SchoolStatus.ACTIVE);
        long suspendedSchools = schoolRepository.countByStatus(SchoolStatus.SUSPENDED);
        long deactivatedSchools = schoolRepository.countByStatus(SchoolStatus.DEACTIVATED);

        long queuedImportJobs = importJobRepository.countByStatus(ImportJobStatus.QUEUED);
        long processingImportJobs = importJobRepository.countByStatus(ImportJobStatus.PROCESSING);
        long failedImportJobs = importJobRepository.countByStatus(ImportJobStatus.FAILED);

        return new PlatformKpiResponse(
                totalSchools,
                activeSchools,
                suspendedSchools,
                deactivatedSchools,
                queuedImportJobs,
                processingImportJobs,
                failedImportJobs
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PlatformImportJobResponse> listImportJobs(
            ImportJobStatus status,
            ImportJobType jobType,
            UUID tenantId,
            LocalDateTime createdAfter,
            Pageable pageable
    ) {
        Page<ImportJob> page = importJobRepository.searchPlatformJobs(status, jobType, tenantId, createdAfter, pageable);
        Map<UUID, String> schoolNamesById = schoolRepository.findAllById(
                        page.getContent().stream().map(ImportJob::getTenantId).distinct().toList()
                ).stream()
                .collect(java.util.stream.Collectors.toMap(School::getId, School::getName));

        return page.map(job -> toPlatformImportJobResponse(job, schoolNamesById.get(job.getTenantId())));
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformImportJobResponse getImportJob(UUID jobId) {
        ImportJob job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Import job not found: " + jobId));
        String schoolName = schoolRepository.findByIdCrossTenant(job.getTenantId())
                .map(School::getName)
                .orElse("Unknown school");
        return toPlatformImportJobResponse(job, schoolName);
    }

    @Override
    public SchoolSummaryResponse updateSchoolStatus(UUID schoolId, SchoolStatus status, String reason, UUID adminId) {
        log.info("Platform admin {} updating school {} status to {} with reason: '{}'", adminId, schoolId, status, reason);
        School school = schoolRepository.findByIdCrossTenant(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found: " + schoolId));

        SchoolStatus previous = school.getStatus();
        school.setStatus(status);
        school = schoolRepository.save(school);

        auditService.log(
                AuditAction.UPDATE,
                "SCHOOL",
                schoolId,
                String.format("School status changed from %s to %s by platform admin %s. Reason: %s", previous, status, adminId, reason)
        );

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
                    "An account with this email already exists "
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
                school.getStatus(),
                school.getSubdomainPrefix(),
                school.getCustomDomain(),
                school.getCreatedAt(),
                school.getProvisionedBy()
        );
    }

    private PlatformImportJobResponse toPlatformImportJobResponse(ImportJob job, String schoolName) {
        int progressPercent = job.getTotalRows() == 0
                ? 0
                : (int) ((job.getProcessedRows() * 100.0) / job.getTotalRows());

        return new PlatformImportJobResponse(
                job.getId(),
                job.getTenantId(),
                schoolName,
                job.getJobType(),
                job.getStatus(),
                job.getTotalRows(),
                job.getProcessedRows(),
                job.getSuccessRows(),
                job.getErrorRows(),
                progressPercent,
                job.getCreatedAt(),
                job.getCompletedAt()
        );
    }
}
