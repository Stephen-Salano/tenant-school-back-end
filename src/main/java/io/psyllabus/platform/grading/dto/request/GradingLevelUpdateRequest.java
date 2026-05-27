package io.psyllabus.platform.grading.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record GradingLevelUpdateRequest(
        @NotBlank(message = "Level label is required")
        String label,

        @NotNull(message = "Minimum percentage is required")
        @DecimalMin(value = "0.00", message = "Minimum percentage cannot be below 0")
        @DecimalMax(value = "100.00", message = "Minimum percentage cannot exceed 100")
        BigDecimal minPercentage,

        @NotNull(message = "Maximum percentage is required")
        @DecimalMin(value = "0.00", message = "Maximum percentage cannot be below 0")
        @DecimalMax(value = "100.00", message = "Maximum percentage cannot exceed 100")
        BigDecimal maxPercentage,

        @NotNull(message = "Sort order is required")
        @PositiveOrZero(message = "Sort order must be zero or greater")
        Integer sortOrder,

        String description
) {}