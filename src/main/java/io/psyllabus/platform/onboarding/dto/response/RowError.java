package io.psyllabus.platform.onboarding.dto.response;

/**
 * One validation error tied to a specific CSV row and column.
 *
 * @param rowNumber 1-based row number (excluding header)
 * @param column    CSV column name where the error was found; null for row-level errors
 * @param issue     human-readable description safe to return to admin
 * @param rawValue  the offending value as it appeared in the CSV
 */
public record RowError(
    int rowNumber,
    String column,
    String issue,
    String rawValue
) {}
