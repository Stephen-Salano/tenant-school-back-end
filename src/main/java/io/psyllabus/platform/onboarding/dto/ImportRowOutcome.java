package io.psyllabus.platform.onboarding.dto;

/**
 * Captures the outcome of processing a single row during bulk onboarding.
 *
 * @param row 1-based row number in the original CSV
 * @param status SUCCESS or FAILED
 * @param finalIdentifier The generated or provided identifier (Admission No, Employee No)
 * @param message Success or failure message
 */
public record ImportRowOutcome(
        int row,
        String status,
        String finalIdentifier,
        String message
) {}
