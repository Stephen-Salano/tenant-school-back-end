package io.psyllabus.platform.school.service;

import io.psyllabus.platform.school.dto.request.SchoolCreateRequest;
import io.psyllabus.platform.school.dto.responses.SchoolResponse;

import java.util.UUID;

public interface SchoolService {

    /**
     * Onboard a new school (tenant).
     * Creates School + Subscription + Admin User + Main Branch.
     *
     * @param request school creation details
     * @return created school details
     */
    SchoolResponse onboardSchool(SchoolCreateRequest request);

    /**
     * Find school by subdomain prefix
     */
    SchoolResponse findBySubdomain(String subdomainPrefix);

    /**
     * Find school by ID
     */
    SchoolResponse findById(UUID schoolId);
}
