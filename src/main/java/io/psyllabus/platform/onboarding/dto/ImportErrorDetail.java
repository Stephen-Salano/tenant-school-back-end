package io.psyllabus.platform.onboarding.dto;

/**
 * One error entry inside {@code ImportJob.errorDetails} (stored as JSONB array).
 *
 * @param row 1-based row number in the original CSV
 * @param field column name that caused the failure (nullable for row-level errors)
 * @param issue human-readable description of the problem
 * @param rawValue the offending value as it appeared in the CSV
 */
public record ImportErrorDetail(
        int row,
        String field,
        String issue,
        String rawValue
) {}
