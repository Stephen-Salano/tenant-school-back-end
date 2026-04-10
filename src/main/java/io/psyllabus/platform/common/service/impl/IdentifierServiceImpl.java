package io.psyllabus.platform.common.service.impl;

import io.psyllabus.platform.common.service.IdentifierService;
import io.psyllabus.platform.staff.repository.StaffRepository;
import io.psyllabus.platform.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdentifierServiceImpl implements IdentifierService {

    private static final int MAX_RETRIES = 5;
    private static final int UPPER_BOUND = 9_000_000; // 1,000,000 to 9,999,999
    private static final int LOWER_BOUND = 1_000_000;
    private static final String STUDENT_PREFIX = "S";
    private static final String STAFF_PREFIX = "T";

    private final SecureRandom random = new SecureRandom();
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;

    // ── Public API ───────────────────────────────────────────────────────────

    @Override
    public String generateStudentId(UUID tenantId) {
        return generate(STUDENT_PREFIX, tenantId);
    }

    @Override
    public String generateStaffId(UUID tenantId) {
        return generate(STAFF_PREFIX, tenantId);
    }

    @Override
    public boolean validate(String identifier) {
        if (identifier == null || identifier.length() != 9) {
            return false;
        }
        String digits = identifier.substring(1); // strip "S" or "T"
        if (!digits.matches("\\d{8}")) {
            return false; // Full 8-digit number must satisfy Luhn (check digit included)
        }
        return luhnSum(digits) % 10 == 0; // key line: valid when total sum mod 10 == 0
    }

    @Override
    public boolean exists(String identifier, UUID tenantId) {
        if (identifier == null || identifier.isBlank()) return false;
        if (identifier.startsWith(STUDENT_PREFIX)) {
            return studentRepository.existsByAdmissionNumberAndTenantId(identifier, tenantId);
        } else if (identifier.startsWith(STAFF_PREFIX)) {
            return staffRepository.existsByEmployeeNumberAndTenantId(identifier, tenantId);
        }
        return false;
    }

    @Override
    public int calculateLuhnCheckDigit(String digits) {
        // Sum the 7 digits as if they occupy positions 2–8 of the final 8-digit number
        // (the check digit will be at position 1 from the right, so it is NOT doubled)
        int sum = luhnSum(digits + "0"); // placeholder "0" for check position
        int remainder = sum % 10;
        return remainder == 0 ? 0 : 10 - remainder; // key line: Luhn check digit formula
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Generate and verify uniqueness within the tenant, retrying up to {@code MAX_RETRIES} times.
     */
    private String generate(String prefix, UUID tenantId) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            String candidate = buildIdentifier(prefix);
            boolean collision = STUDENT_PREFIX.equals(prefix) ?
                    studentRepository.existsByAdmissionNumberAndTenantId(candidate, tenantId) :
                    staffRepository.existsByEmployeeNumberAndTenantId(candidate, tenantId);

            if (!collision) {
                log.debug("Identifier generated [prefix={}, attempt={}, tenantId={}]", prefix, attempt, tenantId);
                return candidate;
            }
            log.warn("Identifier collision [prefix={}, attempt={}, tenantId={}]", prefix, attempt, tenantId);
        }
        // 5 consecutive collisions — something is structurally wrong
        throw new IllegalStateException(
                "Failed to generate unique identifier after " + MAX_RETRIES + " attempts for tenant " + tenantId
        );
    }

    /** Build one candidate: prefix + 7 random digits + Luhn check digit. */
    private String buildIdentifier(String prefix) {
        int randomPart = LOWER_BOUND + random.nextInt(UPPER_BOUND); // 7-digit range
        String digits = String.valueOf(randomPart); // guaranteed 7 chars
        int checkDigit = calculateLuhnCheckDigit(digits);
        return prefix + digits + checkDigit; // e.g. S84920153
    }

    /**
     * Compute the Luhn sum for a digit string.
     * Doubles every second digit from the right; subtracts 9 if the doubled value exceeds 9.
     *
     * @param digits digit string of any even or odd length
     * @return the raw Luhn sum (caller applies {@code % 10} check)
     */
    private int luhnSum(String digits) {
        int sum = 0;
        boolean doubleIt = false; // rightmost digit is never doubled
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = Character.getNumericValue(digits.charAt(i));
            if (doubleIt) {
                d *= 2;
                if (d > 9) {
                    d -= 9; // key line: digit sum shortcut
                }
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return sum;
    }
}
