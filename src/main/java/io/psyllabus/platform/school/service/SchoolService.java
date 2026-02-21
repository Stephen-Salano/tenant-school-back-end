package io.psyllabus.platform.school.service;

import io.psyllabus.platform.school.dto.request.SchoolCreateRequest;
import io.psyllabus.platform.school.dto.responses.SchoolOnboardResponse;
import io.psyllabus.platform.school.dto.responses.SchoolResponse;

import java.util.UUID;

public interface SchoolService {

    /**
     * Provision a new school (tenant).
     * Creates: School → Subscription → Main Branch → Admin User (in that order).
     * Wrapped in a single transaction — rolls back entirely if any step fails.
     *
     * @return one-time provisioning response including plaintext admin credentials
     */
    SchoolOnboardResponse onboardSchool(SchoolCreateRequest request);

    /**
     * Used by TenantResolutionService — resolves school by subdomain on every request.
     */
    SchoolResponse findBySubdomain(String subdomainPrefix);

    /**
     * Authenticated school admin views their school profile.
     */
    SchoolResponse findById(UUID schoolId);
}
