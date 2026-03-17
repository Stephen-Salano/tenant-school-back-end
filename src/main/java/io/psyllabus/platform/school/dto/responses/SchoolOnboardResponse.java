package io.psyllabus.platform.school.dto.responses;

import io.psyllabus.platform.subscription.enums.PlanType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Returned ONCE when a school is provisioned.
 *
 * <p>Contains the admin's temp password in plain text — shown only in this response.
 * The hash is stored in the users table. If credentials are lost, use password reset.
 * It answers: "what just got created and how does the school log in for the first time?"</p>
 *
 * <p>Platform admin hands these to the school contact to begin Phase 2 (Setup wizard).</p>
 *
 * <p>K-DPA: {@code adminTempPassword} is sensitive — this response must not be
 * logged, cached, or stored. The platform admin treats it as a one-time secret.
 * The school admin is forced to change it on first login ({@code mustChangePassword = true}).</p>
 *
 * <p>{@code provisionedByAdminId} is {@code null} when this response is returned from
 * {@code SchoolServiceImpl} directly (e.g. DevDataSeeder). It is stamped by
 * {@code PlatformAdminServiceImpl} after the school record is created, then returned
 * to the platform admin controller as a rebuilt record.</p>
 *
 * @param schoolId             UUID of the provisioned school
 * @param schoolName           name of the school
 * @param subdomainPrefix      subdomain slug, or {@code null} if using a custom domain
 * @param adminEmail           email of the initial school admin account
 * @param adminTempPassword    plaintext temp password — change on first login enforced
 * @param portalLoginUrl       the school's portal URL
 * @param planType             subscription plan that was activated
 * @param provisionedAt        timestamp of provisioning (UTC)
 * @param provisionedByAdminId UUID of the platform admin who provisioned the school,
 *                             or {@code null} when provisioned via seeder
 */
public record SchoolOnboardResponse(
        UUID schoolId,
        String schoolName,
        String subdomainPrefix,
        String adminEmail,
        String adminTempPassword, // Plain text, shown ONCE
        String portalLoginUrl, // e.g https://stmary.psyllabus.co.ke
        PlanType planType,
        LocalDateTime provisionedAt,
        UUID provisionedByAdmin
) {
}
