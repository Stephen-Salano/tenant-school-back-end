package io.psyllabus.platform.common.exception;

import io.psyllabus.platform.onboarding.dto.response.ValidationReport;
import lombok.Getter;

/**
 * Thrown when Phase 1 or Phase 2 CSV validation fails.
 * Caught by {@code GlobalExceptionHandler} → HTTP 422 with full {@link ValidationReport}.
 */
@Getter
public class CsvValidationException extends RuntimeException {
    private final ValidationReport report;

    public CsvValidationException(ValidationReport report) {
        super("CSV validation failed: " + report.errorCount() + " error(s)");
        this.report = report;
    }
}
