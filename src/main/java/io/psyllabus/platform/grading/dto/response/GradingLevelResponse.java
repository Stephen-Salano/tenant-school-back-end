package io.psyllabus.platform.grading.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record GradingLevelResponse(
        UUID id,
        String label,
        BigDecimal minPercentage,
        BigDecimal maxPercentage,
        Integer sortOrder,
        String description
) {
}
