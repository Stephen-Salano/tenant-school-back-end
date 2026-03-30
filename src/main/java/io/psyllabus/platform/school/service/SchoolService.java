package io.psyllabus.platform.school.service;

import io.psyllabus.platform.school.dto.request.SchoolCreateRequest;
import io.psyllabus.platform.school.dto.request.SchoolProfileUpdateRequest;
import io.psyllabus.platform.school.dto.responses.SchoolOnboardResponse;
import io.psyllabus.platform.school.dto.responses.SchoolProfileResponse;
import io.psyllabus.platform.school.entities.School;

import java.util.UUID;

public interface SchoolService {

    /**
     * Provision a new school (tenant).
     * Creates: School → Subscription → Main Branch → Admin User (in that order).
     * Wrapped in a single transaction — rolls back entirely if any step fails.
     *
     * @param request provisioning details from the platform admin
     * @return one-time provisioning response including plaintext admin credentials
     */
    SchoolOnboardResponse onboardSchool(SchoolCreateRequest request);

    /**
     * Resolve a school by its subdomain prefix.
     *
     * <p><strong>Internal infrastructure method</strong> — called by {@code TenantFilter}
     * on every authenticated request to populate {@code TenantContext}.
     * Returns the {@link School} entity directly because no DTO translation is needed
     * for internal tenant resolution; the caller only needs the school ID and active flag.</p>
     *
     * @param subdomainPrefix the subdomain slug (e.g. {@code "riverside"})
     * @return the matching {@link School} entity
     * @throws io.psyllabus.platform.common.exception.ResourceNotFoundException if not found
     */
    School findBySubdomain(String subdomainPrefix);

    /**
     * Fetch the full school profile for the authenticated school admin.
     *
     * <p>Combines fields from the {@link io.psyllabus.platform.school.entities.School}
     * entity (identity) and {@link io.psyllabus.platform.school.entities.SchoolProfile}
     * (settings tabs) into a single response.
     * Result is cached under {@code schoolProfiles:{schoolId}} with a 10-minute TTL.</p>
     *
     * <p>The caller ({@code SchoolController}) validates that {@code schoolId} equals
     * {@code TenantContext.getCurrentTenantId()} before invoking this method —
     * an admin cannot request another school's profile.</p>
     *
     * @param schoolId the tenant UUID (must match the current TenantContext)
     * @return the full profile response including all settings tabs
     * @throws io.psyllabus.platform.common.exception.ResourceNotFoundException if school not found
     */
    SchoolProfileResponse getSchoolProfile(UUID schoolId);

    /**
     * Apply a partial update to the school profile (PATCH semantics).
     *
     * <p>Only sections included in the request are applied. A null section leaves
     * the corresponding tab untouched. Within column-mapped sections (General, Academic,
     * Enrollment), only non-null fields overwrite existing values.
     * JSONB-mapped sections (Facilities, Technology, Policies, Communication) are
     * replaced at the tab level when present — this matches the UI's tab-level save.</p>
     *
     * <p>Throws {@link io.psyllabus.platform.common.exception.InsufficientPermissionException}
     * if {@code schoolId} does not match {@code TenantContext.getCurrentTenantId()}.</p>
     *
     * @param schoolId the tenant UUID (must match the current TenantContext)
     * @param request  the partial update — null sections are skipped entirely
     * @return the updated full profile response
     */
    SchoolProfileResponse updateSchoolProfile(UUID schoolId, SchoolProfileUpdateRequest request);
}
