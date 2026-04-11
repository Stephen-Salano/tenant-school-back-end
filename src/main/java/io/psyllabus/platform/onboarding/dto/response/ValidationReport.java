package io.psyllabus.platform.onboarding.dto.response;

import java.util.List;

/**
 * Returned immediately when either validation phase fails.
 * Frontend renders the error table — admin fixes and re-uploads.
 *
 * @param valid      false if any row has an error
 * @param totalRows  total data rows parsed (excluding header/instruction rows)
 * @param errorCount number of rows with at least one error
 * @param errors     all row-level errors across both phases
 */
public record ValidationReport(
    boolean valid,
    int totalRows,
    int errorCount,
    List<RowError> errors
) {
    public static ValidationReport ok(int totalRows) {
        return new ValidationReport(true, totalRows, 0, List.of());
    }

    public static ValidationReport failed(int totalRows, List<RowError> errors) {
        return new ValidationReport(false, totalRows, errors.size(), errors);
    }
}
