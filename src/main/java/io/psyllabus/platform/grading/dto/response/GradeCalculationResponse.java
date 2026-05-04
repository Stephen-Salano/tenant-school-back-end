package io.psyllabus.platform.grading.dto.response;

import io.psyllabus.platform.grading.enums.GradingSystem;

import java.math.BigDecimal;
import java.util.UUID;

public record GradeCalculationResponse(
        UUID schemeId,
        String schemeName,
        GradingSystem gradingSystem,
        String label,
        BigDecimal percentage
) {
}
