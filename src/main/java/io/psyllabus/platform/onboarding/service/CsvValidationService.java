package io.psyllabus.platform.onboarding.service;

import io.psyllabus.platform.onboarding.dto.response.ValidationReport;
import io.psyllabus.platform.onboarding.enums.ImportJobType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Two-phase CSV validation for bulk import.
 *
 * <p>Phase 1 is pure in-memory — zero DB calls. Run it first and return
 * immediately on failure. Phase 2 makes DB calls only if Phase 1 passes.
 * This keeps the common case (bad CSV format) fast and cheap.
 */
public interface CsvValidationService {

    /**
     * Phase 1 — Format validation. Zero DB calls.
     * Checks: required columns, date formats, enum values, email/phone patterns, row count.
     *
     * @param rows    parsed CSV rows (header stripped)
     * @param jobType determines which required columns and rules to apply
     * @return report with {@code valid = true} if all rows pass, otherwise all errors found
     */
    ValidationReport phaseOne(List<Map<String, String>> rows, ImportJobType jobType);

    /**
     * Phase 2 — Reference validation. Makes DB calls, but batched — not per row.
     * Checks: branch codes, SchoolClass existence, SubjectGroup existence, duplicate identifiers.
     *
     * <p>Call only after {@link #phaseOne} returns {@code valid = true}.
     *
     * @param rows    the same rows passed to Phase 1
     * @param jobType determines which reference checks to run
     * @param tenantId scopes all DB lookups to this tenant
     * @return report with {@code valid = true} if all reference data resolves
     */
    ValidationReport phaseTwo(List<Map<String, String>> rows, ImportJobType jobType, UUID tenantId);
}
