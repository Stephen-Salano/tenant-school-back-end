package io.psyllabus.platform.staff.dto.request;

import io.psyllabus.platform.staff.enums.EmploymentStatus;
import jakarta.validation.constraints.NotNull;

/**
 * PATCH /api/staff/{id}/status
 * Admin transitions an employee's employment status.
 *
 * reason is optional — used for audit context (e.g. "Annual contract review", "Resignation").
 * Not persisted on Staff entity; will feed into the audit trail sprint.
 */
public record UpdateStaffStatusRequest(
  @NotNull(message = "Status is required")
  EmploymentStatus status,
  String reason
) {
}
