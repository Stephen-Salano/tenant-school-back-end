package io.psyllabus.platform.platformadmin.services;

import io.psyllabus.platform.platformadmin.dto.request.PlatformAdminCreateRequest;
import io.psyllabus.platform.onboarding.enums.ImportJobStatus;
import io.psyllabus.platform.onboarding.enums.ImportJobType;
import io.psyllabus.platform.school.dto.request.SchoolCreateRequest;
import io.psyllabus.platform.platformadmin.dto.responses.PlatformImportJobResponse;
import io.psyllabus.platform.platformadmin.dto.responses.PlatformKpiResponse;
import io.psyllabus.platform.platformadmin.dto.responses.PlatformAdminResponse;
import io.psyllabus.platform.school.dto.responses.SchoolOnboardResponse;
import io.psyllabus.platform.school.dto.responses.SchoolSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service contract for platform-level administration.
 *
 * <p>All methods operate <strong>across tenant boundaries</strong> by design.
 * This is the only service explicitly permitted to bypass {@code TenantContext}.
 * Every other service must scope its queries to the current tenant.</p>
 *
 * <p><strong>Why the exception is safe:</strong></p>
 * <ul>
 *   <li>All methods are protected by {@code @PreAuthorize("hasRole('PLATFORM_ADMIN')")}
 *       at the controller layer.</li>
 *   <li>{@code PlatformAdminAuthFilter} validates the JWT and loads the principal from
 *       the {@code platform_admins} table before any method here is reached.</li>
 * </ul>
 */
public interface PlatformAdminService {

    /**
     * Provision a new school end-to-end.
     *
     * <p>Delegates to {@code SchoolService.onboardSchool()} which creates the school,
     * subscription, main branch, and admin user. After {@code onboardSchool()} completes,
     * this method stamps {@code provisionedBy} on the {@code School} entity and returns
     * a rebuilt {@link SchoolOnboardResponse} with {@code provisionedByAdminId} set.</p>
     *
     * <p>Cross-tenant: creation operation — no existing tenant context required.</p>
     *
     * @param request  provisioning parameters; must include exactly one of
     *                 {@code subdomainPrefix} or {@code customDomain}
     * @param adminId  UUID of the platform admin performing provisioning —
     *                 written to {@code School.provisionedBy} for the audit trail
     * @return provisioning response including the one-time temp admin password
     * @throws io.psyllabus.platform.common.exception.DuplicateResourceException if the
     *         subdomain or email is already registered
     * @throws io.psyllabus.platform.common.exception.ValidationException if neither or
     *         both domain fields are supplied
     */
    SchoolOnboardResponse provisionSchool(SchoolCreateRequest request, UUID adminId);

    /**
     * List all schools across all tenants, paginated.
     *
     * <p><strong>Intentionally cross-tenant</strong> — no {@code TenantContext} read.
     * No {@code WHERE tenant_id = ?} filter applied. Correct behaviour for a
     * platform-level oversight view.</p>
     *
     * @param pageable pagination and sort parameters
     * @return page of school summaries across all tenants
     */
    Page<SchoolSummaryResponse> listAllSchools(Pageable pageable);

    /**
     * Fetch any school's profile regardless of tenant.
     *
     * <p><strong>Intentionally cross-tenant</strong> — no {@code TenantContext} read.</p>
     *
     * @param schoolId UUID of the school to fetch
     * @return school summary
     * @throws io.psyllabus.platform.common.exception.ResourceNotFoundException if not found
     */
    SchoolSummaryResponse getAnySchool(UUID schoolId);

    PlatformKpiResponse getDashboardKpis();

    Page<PlatformImportJobResponse> listImportJobs(
            ImportJobStatus status,
            ImportJobType jobType,
            UUID tenantId,
            LocalDateTime createdAfter,
            Pageable pageable
    );

    PlatformImportJobResponse getImportJob(UUID jobId);

    /**
     * Update a school's status (ACTIVE, SUSPENDED, DEACTIVATED).
     *
     * <p><strong>Intentionally cross-tenant</strong> — no {@code TenantContext} read.</p>
     *
     * @param schoolId UUID of the school to update
     * @param status   the new status (ACTIVE, SUSPENDED, DEACTIVATED)
     * @param adminId  the platform admin performing the update (for auditing)
     * @return updated school summary
     */
    SchoolSummaryResponse updateSchoolStatus(UUID schoolId, io.psyllabus.platform.school.enums.SchoolStatus status, UUID adminId);

    /**
     * Create a new platform-level administrator.
     *
     * <p>Stored in {@code platform_admins} — separate from school users.
     * Created with {@code ACTIVE} status and no tenant association.</p>
     *
     * @param request account details for the new platform admin
     * @return the created admin's profile (no password in response)
     * @throws io.psyllabus.platform.common.exception.DuplicateResourceException if
     *         username or email is already registered
     */
    PlatformAdminResponse createPlatformAdmin(PlatformAdminCreateRequest request);

}
