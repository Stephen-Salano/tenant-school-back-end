package io.psyllabus.platform.common.service;

import java.util.UUID;

/**
 * Generates and validates Luhn-check admission/employee numbers.
 *
 * <p>Format: {@code [S|T] + 7 random digits + 1 Luhn check digit} = 9 characters total. </p>
 * <ul>
 * <li>Sequential IDs (ADM-001, ADM-002) expose enrollment count and year.
 * Luhn random IDs reveal nothing.</li>
 * <li>The check digit lets the frontend reject mistyped IDs instantly —
 * no server round-trip required.</li>
 * <li>Uniqueness is per-tenant only — the same number can exist in two different schools.</li>
 * </ul>
 */
public interface IdentifierService {

    /**
     * Generate a unique student admission number for the given tenant.
     *
     * @param tenantId the school's UUID — uniqueness is scoped to this tenant
     * @return a 9-character string in the form {@code S########}
     * @throws IllegalStateException if 5 consecutive collision retries all fail
     */
    String generateStudentId(UUID tenantId);

    /**
     * Generate a unique staff employee number for the given tenant.
     *
     * @param tenantId the school's UUID
     * @return a 9-character string in the form {@code T########}
     * @throws IllegalStateException if 5 consecutive collision retries all fail
     */
    String generateStaffId(UUID tenantId);

    /**
     * Validate an identifier using the Luhn algorithm.
     * Pure computation — makes no database calls.
     *
     * @param identifier the full identifier including prefix, e.g. {@code S84920153}
     * @return {@code true} if the check digit is valid
     */
    boolean validate(String identifier);

    /**
     * Check if an identifier exists within a tenant.
     *
     * @param identifier the full identifier (S... or T...)
     * @param tenantId school UUID
     * @return true if exists
     */
    boolean exists(String identifier, UUID tenantId);

    /**
     * Calculate the Luhn check digit for a string of digits.
     *
     * @param digits exactly 7 digit characters
     * @return check digit in range [0, 9]
     */
    int calculateLuhnCheckDigit(String digits);
}
