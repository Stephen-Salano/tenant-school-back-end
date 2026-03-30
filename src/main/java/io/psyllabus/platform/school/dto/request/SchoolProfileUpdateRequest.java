package io.psyllabus.platform.school.dto.request;

import io.psyllabus.platform.school.enums.CurriculumType;
import io.psyllabus.platform.school.enums.GenderPolicy;
import io.psyllabus.platform.school.enums.SchoolType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.List;

/**
 * PATCH request for updating school profile settings.
 *
 * <p>All top-level sections are nullable — only sections included in the request
 * are applied. Sending only the {@code general} section leaves all other tabs
 * untouched. This matches the tab-by-tab save behaviour in the settings UI.</p>
 *
 * <p>Each nested record mirrors one settings tab exactly. The nested record
 * defines the allowed keys and their types — unknown JSON keys from the frontend
 * are silently ignored by Jackson. This is where the JSONB shape is enforced:
 * the database stores whatever arrives, but only valid structures ever arrive.</p>
 */

public record SchoolProfileUpdateRequest(

        @Valid GeneralSection general,
        @Valid AcademicSection academic,
        @Valid EnrollmentSection enrollment,
        @Valid FacilitiesSection facilities,
        @Valid TechnologySection technology,
        @Valid PoliciesSection policies,
        @Valid CommunicationSection communication
) {
    // ── General tab ──────────────────────────────────────────────────────────

    /**
     * General information and branding settings.
     * All fields are optional — only non-null values are applied (PATCH semantics).
     */
    public record GeneralSection(

            @Size(max = 20, message = "School code must not exceed 20 characters")
            String schoolCode,

            SchoolType schoolType,

            GenderPolicy genderPolicy,

            @Size(max = 512, message = "Logo URL must not exceed 512 characters")
            String logoUrl,

            @Size(max = 255, message = "Website must not exceed 255 characters")
            String website,

            @Min(value = 1800, message = "Established year must be 1800 or later")
            @Max(value = 2100, message = "Established year must be 2100 or earlier")
            Integer establishedYear,

            @Size(max = 255, message = "Motto must not exceed 255 characters")
            String motto,

            String vision,

            String mission,

            /**
             * Brand colors map. Expected keys: {@code "primary"}, {@code "secondary"}.
             * Values must be valid hex color strings (e.g. {@code "#3B82F6"}).
             */
            java.util.Map<
                    @Size(max = 20) String,
                    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Brand color must be a valid hex color e.g. #3B82F6") String
                    > brandColors

    ) {}

    // ── Academic tab ─────────────────────────────────────────────────────────

    /**
     * Academic calendar and curriculum settings.
     */
    public record AcademicSection(

            CurriculumType curriculumType,

            @Size(max = 50, message = "School days description must not exceed 50 characters")
            String schoolDays,

            LocalTime startTime,

            LocalTime endTime,

            LocalTime lunchStartTime,

            LocalTime lunchEndTime,

            Boolean useSystemLearningAreas

    ) {}

    // ── Enrollment tab ───────────────────────────────────────────────────────

    /**
     * Enrollment capacity and grade settings.
     *
     * <p>Note: {@code currentEnrollment} and {@code availableSlots} are read-only —
     * they are computed at runtime from the students table and are never accepted
     * in a write request.</p>
     */
    public record EnrollmentSection(

            @Min(value = 1, message = "Total capacity must be at least 1")
            Integer totalCapacity,

            @Min(value = 1, message = "Max class size must be at least 1")
            Integer maxClassSize,

            @Min(value = 1, message = "Student-teacher ratio must be at least 1")
            Integer studentTeacherRatio,

            @Min(value = 1, message = "Average class size must be at least 1")
            Integer averageClassSize,

            /**
             * List of grade labels this school admits.
             * e.g. {@code ["Grade 1", "Grade 2", ..., "Grade 8"]}
             */
            List<@Size(max = 50) String> gradesOffered

    ) {}

    // ── Facilities tab ───────────────────────────────────────────────────────

    /**
     * Infrastructure and facilities settings.
     * Toggles map to {@code Boolean}, counts map to {@code Integer},
     * tag-input lists map to {@code List<String>}.
     */
    public record FacilitiesSection(

            @Min(0) Integer classrooms,
            @Min(0) Integer laboratories,
            @Min(0) Integer libraries,
            @Min(0) Integer computerLabs,

            Boolean cafeteria,
            Boolean auditorium,
            Boolean medicalCenter,
            Boolean boardingFacilities,
            Boolean transportServices,
            Boolean internetAccess,

            /** Tag-input list — user types and adds values freely. */
            List<@Size(max = 100) String> sportsFacilities,

            @Size(max = 500, message = "Security description must not exceed 500 characters")
            String securityDescription

    ) {}

    // ── Technology tab ───────────────────────────────────────────────────────

    /**
     * IT infrastructure and technology settings.
     */
    public record TechnologySection(

            @Size(max = 100, message = "LMS name must not exceed 100 characters")
            String lms,

            @Size(max = 100, message = "SIS name must not exceed 100 characters")
            String studentInformationSystem,

            @Min(0) Integer smartBoards,

            @Size(max = 20, message = "Computer ratio must not exceed 20 characters")
            String computerRatio,

            @Size(max = 50, message = "Internet speed must not exceed 50 characters")
            String internetSpeed,

            Boolean wifiAvailable,

            @Size(max = 50, message = "Computer lab hours must not exceed 50 characters")
            String computerLabHours,

            @Size(max = 200, message = "Tech support description must not exceed 200 characters")
            String techSupport,

            /** Tag-input list — user types and adds values freely. */
            List<@Size(max = 100) String> softwareLicenses

    ) {}

    // ── Policies tab ─────────────────────────────────────────────────────────

    /**
     * School policies and regulations.
     * All fields are free-text — no enum needed since these are human-written policies.
     */
    public record PoliciesSection(

            @Size(max = 1000) String attendancePolicy,
            @Size(max = 1000) String dressCode,
            @Size(max = 1000) String disciplinePolicy,
            @Size(max = 1000) String homeworkPolicy,
            @Size(max = 1000) String bullyingPolicy,
            @Size(max = 1000) String emergencyProcedures,
            @Size(max = 1000) String parentInvolvement,

            /** Tag-input list — user types and adds values freely. */
            List<@Size(max = 100) String> extracurricularActivities

    ) {}

    // ── Communication tab ────────────────────────────────────────────────────

    /**
     * Communication preferences and notification settings.
     *
     * <p>Dropdown fields (e.g. {@code newsletterFrequency}) use {@code String} rather
     * than Java enums because they live inside a JSONB blob — no field-by-field DB
     * query ever runs on them. Allowed values are enforced here via {@code @Pattern}.</p>
     */
    public record CommunicationSection(

            Boolean parentPortalEnabled,
            Boolean emailNotificationsEnabled,
            Boolean smsAlertsEnabled,

            /** Tag-input list — user types and adds values freely. */
            List<@Size(max = 100) String> socialMediaPlatforms,

            /**
             * Allowed values: {@code WEEKLY}, {@code BI_WEEKLY}, {@code MONTHLY}, {@code QUARTERLY}.
             */
            @Pattern(
                    regexp = "^(WEEKLY|BI_WEEKLY|MONTHLY|QUARTERLY)$",
                    message = "newsletterFrequency must be one of: WEEKLY, BI_WEEKLY, MONTHLY, QUARTERLY"
            )
            String newsletterFrequency,

            /**
             * Allowed values: {@code END_OF_TERM}, {@code MID_AND_END_OF_TERM}, {@code MONTHLY}.
             */
            @Pattern(
                    regexp = "^(END_OF_TERM|MID_AND_END_OF_TERM|MONTHLY)$",
                    message = "reportCards must be one of: END_OF_TERM, MID_AND_END_OF_TERM, MONTHLY"
            )
            String reportCards,

            /**
             * Allowed values: {@code WEEKLY}, {@code MONTHLY}, {@code ONCE_PER_TERM}, {@code TWICE_PER_TERM}.
             */
            @Pattern(
                    regexp = "^(WEEKLY|MONTHLY|ONCE_PER_TERM|TWICE_PER_TERM)$",
                    message = "parentTeacherMeetings must be one of: WEEKLY, MONTHLY, ONCE_PER_TERM, TWICE_PER_TERM"
            )
            String parentTeacherMeetings,

            /**
             * Allowed values: {@code MID_TERM}, {@code WEEKLY}, {@code MONTHLY}, {@code AS_NEEDED}.
             */
            @Pattern(
                    regexp = "^(MID_TERM|WEEKLY|MONTHLY|AS_NEEDED)$",
                    message = "progressReports must be one of: MID_TERM, WEEKLY, MONTHLY, AS_NEEDED"
            )
            String progressReports

    ) {}
}
