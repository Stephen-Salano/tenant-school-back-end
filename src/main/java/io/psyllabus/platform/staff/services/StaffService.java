package io.psyllabus.platform.staff.services;

import io.psyllabus.platform.staff.dto.request.StaffCreateRequest;
import io.psyllabus.platform.staff.dto.responses.StaffResponse;

import java.util.UUID;

public interface StaffService {

    /**
     * Create a new staff member (creates User + Staff)
     * @param request the request used to create Staff
     * @return a staff member's details
     */
    StaffResponse createStaff(StaffCreateRequest request);

    /**
     * Assign staff to a department
     * @param staffId the staff members ID number
     * @param departmentId department Id number
     */
    void assignDepartment(UUID staffId, UUID departmentId);

    /**
     * Find a staff by employee number
     * @param employeeNumber the staff member's ID number
     * @param tenantId the school / tenant ID
     * @return The staff members details
     */
    StaffResponse findByEmployeeNumber(String employeeNumber, UUID tenantId);
}
