package io.psyllabus.platform.academic.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ClassUpdateRequest(
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @Size(max = 50, message = "Grade level must not exceed 50 characters")
        String gradeLevel,

        @Size(max = 50, message = "Stream name must not exceed 50 characters")
        String streamName,

        UUID academicYearId,

        UUID branchId,

        @Min(value = 1, message = "Max capacity must be at least 1")
        Integer maxCapacity,

        Boolean isActive
) {
}
