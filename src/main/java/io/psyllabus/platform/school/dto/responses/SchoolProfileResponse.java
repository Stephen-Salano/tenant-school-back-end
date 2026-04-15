package io.psyllabus.platform.school.dto.responses;

import io.psyllabus.platform.school.enums.CurriculumType;
import io.psyllabus.platform.school.enums.GenderPolicy;
import io.psyllabus.platform.school.enums.SchoolStatus;
import io.psyllabus.platform.school.enums.SchoolType;
import io.psyllabus.platform.subscription.enums.PlanType;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Full school profile response — returned by {@code GET /api/schools/me/profile}.
 *
 * <p>Structure mirrors {@code SchoolProfileUpdateRequest} tab-for-tab so the
 * frontend can use the same shape for both reading and writing settings.
 * All nested sections may be {@code null} if the admin has not yet configured
 * that tab — the frontend renders sensible defaults in that case.</p>
 *
 * <p>Read-only computed fields ({@code currentEnrollment}, {@code availableSlots})
 * are included here but are never present in the update request.</p>
 */
public record SchoolProfileResponse(

        // ── School entity fields (always present) ────────────────────────────
        UUID schoolId,
        String name,
        String subdomainPrefix,
        String email,
        String phone,
        String address,
        PlanType plan,
        SchoolStatus status,

        // ── SchoolProfile tab sections (null until admin configures each tab) ─
        GeneralSection general,
        AcademicSection academic,
        EnrollmentSection enrollment,
        FacilitiesSection facilities,
        TechnologySection technology,
        PoliciesSection policies,
        CommunicationSection communication
) {
    // ── General tab ──────────────────────────────────────────────────────────

    public record GeneralSection(
            String schoolCode,
            SchoolType schoolType,
            GenderPolicy genderPolicy,
            String logoUrl,
            String website,
            Integer establishedYear,
            String motto,
            String vision,
            String mission,
            Map<String, String> brandColors
    ) {}

    // ── Academic tab ─────────────────────────────────────────────────────────

    public record AcademicSection(
            CurriculumType curriculumType,
            String schoolDays,
            LocalTime startTime,
            LocalTime endTime,
            LocalTime lunchStartTime,
            LocalTime lunchEndTime,
            boolean useSystemLearningAreas
    ) {}

    // ── Enrollment tab ───────────────────────────────────────────────────────

    /**
     * Includes read-only computed fields not present in the update request.
     *
     * <p>{@code currentEnrollment} = count of active students in the students table.<br>
     * {@code availableSlots} = {@code totalCapacity} - {@code currentEnrollment}.<br>
     * Both are computed by the service — never stored in {@code school_profiles}.</p>
     */
    public record EnrollmentSection(
            Integer totalCapacity,
            Integer currentEnrollment,   // computed — read-only
            Integer availableSlots,      // computed — read-only
            Integer maxClassSize,
            Integer studentTeacherRatio,
            Integer averageClassSize,
            List<String> gradesOffered
    ) {}

    // ── Facilities tab ───────────────────────────────────────────────────────

    public record FacilitiesSection(
            Integer classrooms,
            Integer laboratories,
            Integer libraries,
            Integer computerLabs,
            Boolean cafeteria,
            Boolean auditorium,
            Boolean medicalCenter,
            Boolean boardingFacilities,
            Boolean transportServices,
            Boolean internetAccess,
            List<String> sportsFacilities,
            String securityDescription
    ) {}

    // ── Technology tab ───────────────────────────────────────────────────────

    public record TechnologySection(
            String lms,
            String studentInformationSystem,
            Integer smartBoards,
            String computerRatio,
            String internetSpeed,
            Boolean wifiAvailable,
            String computerLabHours,
            String techSupport,
            List<String> softwareLicenses
    ) {}

    // ── Policies tab ─────────────────────────────────────────────────────────

    public record PoliciesSection(
            String attendancePolicy,
            String dressCode,
            String disciplinePolicy,
            String homeworkPolicy,
            String bullyingPolicy,
            String emergencyProcedures,
            String parentInvolvement,
            List<String> extracurricularActivities
    ) {}

    // ── Communication tab ────────────────────────────────────────────────────

    /**
     * Dropdown values are stored as strings — validated at the request DTO layer
     * via {@code @Pattern}, not enforced as DB enums.
     *
     * <ul>
     *   <li>{@code newsletterFrequency}: WEEKLY, BI_WEEKLY, MONTHLY, QUARTERLY</li>
     *   <li>{@code reportCards}: END_OF_TERM, MID_AND_END_OF_TERM, MONTHLY</li>
     *   <li>{@code parentTeacherMeetings}: WEEKLY, MONTHLY, ONCE_PER_TERM, TWICE_PER_TERM</li>
     *   <li>{@code progressReports}: MID_TERM, WEEKLY, MONTHLY, AS_NEEDED</li>
     * </ul>
     */
    public record CommunicationSection(
            Boolean parentPortalEnabled,
            Boolean emailNotificationsEnabled,
            Boolean smsAlertsEnabled,
            List<String> socialMediaPlatforms,
            String newsletterFrequency,
            String reportCards,
            String parentTeacherMeetings,
            String progressReports
    ) {}
}
