package io.psyllabus.platform.staff.services;

import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.staff.dto.request.StaffCreateRequest;
import io.psyllabus.platform.staff.dto.request.UpdateStaffStatusRequest;
import io.psyllabus.platform.staff.dto.responses.StaffDetailResponse;
import io.psyllabus.platform.staff.dto.responses.StaffResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface StaffService {

    /**
     * Create a new staff member (creates User + Staff)
     * @param request the request used to create Staff
     * @return a staff member's details
     */
    StaffResponse createStaff(StaffCreateRequest request);

    /**
     * Assign staff to a subject group (department equivalent)
     * @param staffId the staff members ID number
     * @param subjectGroupId subject group Id number
     * @return the updated staff response
     */
    StaffResponse assignSubjectGroup(UUID staffId, UUID subjectGroupId);

    /**
     * Find a staff by employee number
     * @param employeeNumber the staff member's ID number
     * @param tenantId the school / tenant ID
     * @return The staff members details
     */
    StaffResponse findByEmployeeNumber(String employeeNumber, UUID tenantId);

    // ── Added Stage 5 ──────────────────────────────────────────────────────────

    /**
     * Paginated list of all staff within the current tenant.
     * Used by ADMIN/STAFF list endpoint.
     */
    PageResponse<StaffResponse> findAll(Pageable pageable);

    /**
     * Fetch full staff detail by ID (tenant-scoped).
     * Returns StaffDetailResponse — richer than the list DTO.
     */
    StaffDetailResponse findById(UUID staffId);

    /**
     * Transition a staff member's employment status.
     * State-aware: caller provides target status + optional reason.
     */
    StaffResponse updateStatus(UUID staffId, UpdateStaffStatusRequest request);
}
