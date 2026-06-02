package io.psyllabus.platform.academic.dto.request;

import jakarta.validation.constraints.Size;

public record ClassUpdateRequest(
        @Size(max = 100)
        String name,

        @Size(max = 50)
        String gradeLevel,

        @Size(max = 50)
        String streamName,

        Integer maxCapacity
) {
}
