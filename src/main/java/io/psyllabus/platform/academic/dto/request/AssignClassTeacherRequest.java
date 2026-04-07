package io.psyllabus.platform.academic.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request body for {@code PUT /api/classes/{id}/class-teacher}.
 *
 * <p>The staff member must already have an active {@code TeacherAssignment}
 * in this class (for any subject). The service promotes that assignment to
 * class teacher and demotes any previous holder.</p>
 */
public record AssignClassTeacherRequest(
    @NotNull(message = "Staff ID is required")
    UUID staffId
) {}
