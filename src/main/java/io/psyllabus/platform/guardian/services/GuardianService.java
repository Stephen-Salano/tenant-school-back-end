package io.psyllabus.platform.guardian.services;

import io.psyllabus.platform.guardian.dto.request.GuardianCreateRequest;
import io.psyllabus.platform.guardian.dto.responses.GuardianResponse;

import java.util.UUID;

public interface GuardianService {

    /**
     * Create a new guardian (creates User + Guardian)
     */
    GuardianResponse createGuardian(GuardianCreateRequest request);

    /**
     * Link guardian to student
     */
    void linkToStudent(UUID guardianId, UUID studentId);

    /**
     * Unlink guardian from student
     */
    void unlinkFromStudent(UUID guardianId, UUID studentId);

    /**
     * Find guardian by email
     */
    GuardianResponse findByEmail(String email, UUID tenantId);
}
