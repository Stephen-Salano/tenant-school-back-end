package io.psyllabus.platform.guardian.services;

import io.psyllabus.platform.guardian.dto.request.GuardianCreateRequest;
import io.psyllabus.platform.guardian.dto.responses.GuardianResponse;

import java.util.List;
import java.util.UUID;

public interface GuardianService {

    /**
     * Create a new guardian (creates User + Guardian)
     */
    GuardianResponse createGuardian(GuardianCreateRequest request);

    /**
     * Link guardian to student
     * @return the updated guardian response
     */
    GuardianResponse linkToStudent(UUID guardianId, UUID studentId);

    /**
     * Unlink guardian from student
     * @return the updated guardian response
     */
    GuardianResponse unlinkFromStudent(UUID guardianId, UUID studentId);

    /**
     * Find guardian by email
     */
    GuardianResponse findByEmail(String email, UUID tenantId);

    /**
     * Return all guardians linked to a specific student (tenant-scoped).
     * Used by ADMIN/STAFF to view a student's family/emergency contacts.
     */
    List<GuardianResponse> findByStudentId(UUID studentId);
}
