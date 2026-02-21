package io.psyllabus.platform.school.dto.responses;

import java.util.UUID;

/**
 * Returned ONCE when a school is provisioned.
 *
 * Contains the admin's temp password in plain text — shown only in this response.
 * The hash is stored in the users table. If credentials are lost, use password reset.
 * It answers "what just got created and how does the school log in for the first time?
 *
 * Platform admin hands these to the school contact to begin Phase 2 (Setup wizard).
 */
public record SchoolOnboardResponse(
        UUID schoolId,
        String schoolName,
        String subdomainPrefix,
        String adminEmail,
        String adminTempPassword, // Plain text, shown ONCE
        String portalLoginUrl // e.g https://stmary.psyllabus.co.ke
) {
}
