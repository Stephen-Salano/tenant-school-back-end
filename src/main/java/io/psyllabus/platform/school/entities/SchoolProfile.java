package io.psyllabus.platform.school.entities;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import io.psyllabus.platform.school.enums.CurriculumType;
import io.psyllabus.platform.school.enums.GenderPolicy;
import io.psyllabus.platform.school.enums.SchoolType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rich school settings — loaded <strong>on demand</strong> (admin settings page only).
 *
 * <p>Uses {@code school_id} as both PK and FK (shared primary key pattern).
 * The lean {@link School} entity stays cacheable and fast on every authenticated request.
 * A JOIN only happens when you explicitly request the profile.</p>
 *
 * <p><strong>Column vs JSONB decision rule applied here:</strong></p>
 * <ul>
 *   <li><strong>Regular column</strong> — backend reads or filters on it
 *       (e.g. {@code curriculumType}, {@code streamNames}, capacity fields).</li>
 *   <li><strong>JSONB blob</strong> — always read and written as a whole unit;
 *       backend never queries individual fields inside it
 *       (e.g. {@code facilities}, {@code technology}, {@code policies}, {@code communication}).</li>
 * </ul>
 *
 * <p>K-DPA: no student or guardian PII is stored here.
 * This entity holds institutional configuration only.</p>
 */
@Entity
@Table(name = "school_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolProfile {

    /**
     * Shared PK/FK with {@code schools} table.
     * Value is always set from the owning {@link School} — never auto-generated.
     */
    @Id
    @Column(name = "school_id")
    private UUID schoolId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "school_id")
    private School school;

    // ── General tab ──────────────────────────────────────────────────────────

    /**
     * Short identifier code for the school (e.g. "RA-001").
     * Displayed on reports and documents. Not used for tenant resolution.
     */
    @Column(name = "school_code", length = 20)
    private String schoolCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "school_type", length = 30)
    private SchoolType schoolType;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_policy", length = 20)
    private GenderPolicy genderPolicy;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "established_year")
    private Integer establishedYear;

    @Column(name = "motto", length = 255)
    private String motto;

    @Column(name = "vision", columnDefinition = "TEXT")
    private String vision;

    @Column(name = "mission", columnDefinition = "TEXT")
    private String mission;

    /**
     * JSONB: brand colours for the school portal UI.
     * Shape: {@code {"primary": "#3B82F6", "secondary": "#3d6c5c"}}
     */
    @Type(JsonBinaryType.class)
    @Column(name = "brand_colors", columnDefinition = "jsonb")
    private Map<String, String> brandColors;

    // ── Academic tab ─────────────────────────────────────────────────────────

    /**
     * Regular column — {@code AcademicTermServiceImpl} reads this to decide
     * how many terms to auto-generate and the default date ranges.
     *
     * @see CurriculumType
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "curriculum_type", length = 20)
    private CurriculumType curriculumType;

    /** E.g. "Monday - Friday". Stored as a descriptive string. */
    @Column(name = "school_days", length = 50)
    private String schoolDays;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    /** Start of the lunch break (e.g. 12:30). */
    @Column(name = "lunch_start_time")
    private LocalTime lunchStartTime;

    /** End of the lunch break (e.g. 13:30). */
    @Column(name = "lunch_end_time")
    private LocalTime lunchEndTime;

    /**
     * When {@code true}, the 7 CBC Learning Areas are seeded automatically
     * when school setup completes. Defaults {@code true} for CBC schools,
     * {@code false} for IGCSE.
     */
    @Column(name = "use_system_learning_areas", nullable = false)
    @Builder.Default
    private boolean useSystemLearningAreas = true;

    // ── Stream names ─────────────────────────────────────────────────────────

    /**
     * JSONB array of valid stream names configured by the school admin.
     * Example: {@code ["Blue", "Red", "Green"]} or {@code ["North", "South"]}.
     *
     * <p>{@code ClassServiceImpl} validates the incoming {@code streamName}
     * against this list on class creation (SM3 issue #16).
     * {@code null} or empty = no validation enforced.</p>
     */
    @Type(JsonBinaryType.class)
    @Column(name = "stream_names", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> streamNames = new java.util.ArrayList<>();

    // ── Enrollment tab ───────────────────────────────────────────────────────
    // Note: currentEnrollment and availableSlots are computed at runtime
    // from the students table — never stored here.

    @Column(name = "total_capacity")
    private Integer totalCapacity;

    @Column(name = "max_class_size")
    private Integer maxClassSize;

    @Column(name = "student_teacher_ratio")
    private Integer studentTeacherRatio;

    @Column(name = "average_class_size")
    private Integer averageClassSize;

    /**
     * JSONB array of grade levels this school offers.
     * Example: {@code ["Grade 1", "Grade 2", ..., "Grade 8"]}
     */
    @Type(JsonBinaryType.class)
    @Column(name = "grades_offered", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> gradesOffered = new java.util.ArrayList<>();

    // ── Facilities tab (JSONB blob) ───────────────────────────────────────────
    /**
     * JSONB: all infrastructure and facilities data.
     *
     * <p>Expected shape (all fields optional):</p>
     * <pre>{@code
     * {
     *   "classrooms": 45,
     *   "laboratories": 8,
     *   "libraries": 2,
     *   "computerLabs": 3,
     *   "cafeteria": true,
     *   "auditorium": true,
     *   "medicalCenter": true,
     *   "boardingFacilities": false,
     *   "transportServices": true,
     *   "internetAccess": true,
     *   "sportsFacilities": ["Football field", "Basketball courts"],
     *   "securityDescription": "24/7 CCTV and security guards"
     * }
     * }</pre>
     */
    @Type(JsonBinaryType.class)
    @Column(name = "facilities", columnDefinition = "jsonb")
    private Map<String, Object> facilities;

    // ── Technology tab (JSONB blob) ───────────────────────────────────────────
    /**
     * JSONB: IT infrastructure and technology settings.
     *
     * <p>Expected shape (all fields optional):</p>
     * <pre>{@code
     * {
     *   "lms": "Moodle",
     *   "studentInformationSystem": "Custom Built",
     *   "smartBoards": 35,
     *   "computerRatio": "1:3",
     *   "internetSpeed": "100 Mbps",
     *   "wifiAvailable": true,
     *   "computerLabHours": "08:00 - 17:00",
     *   "techSupport": "Available during school hours",
     *   "softwareLicenses": ["Microsoft Office", "Adobe Creative Cloud"]
     * }
     * }</pre>
     */
    @Type(JsonBinaryType.class)
    @Column(name = "technology", columnDefinition = "jsonb")
    private Map<String, Object> technology;

    // ── Policies tab (JSONB blob) ─────────────────────────────────────────────
    /**
     * JSONB: school policies and regulations.
     *
     * <p>Expected shape (all fields optional):</p>
     * <pre>{@code
     * {
     *   "attendancePolicy": "Students must maintain 85% attendance rate",
     *   "dressCode": "School uniform mandatory on all school days",
     *   "disciplinePolicy": "Progressive discipline system with counseling",
     *   "homeworkPolicy": "Maximum 2 hours of homework per day",
     *   "bullyingPolicy": "Zero tolerance for bullying",
     *   "emergencyProcedures": "Regular fire drills and emergency response training",
     *   "parentInvolvement": "Parent-teacher meetings once per term",
     *   "extracurricularActivities": ["Sports", "Music", "Drama", "Debate Club"]
     * }
     * }</pre>
     */
    @Type(JsonBinaryType.class)
    @Column(name = "policies", columnDefinition = "jsonb")
    private Map<String, Object> policies;

    // ── Communication tab (JSONB blob) ────────────────────────────────────────
    /**
     * JSONB: communication preferences and notification settings.
     *
     * <p>Expected shape (all fields optional):</p>
     * <pre>{@code
     * {
     *   "parentPortalEnabled": true,
     *   "emailNotificationsEnabled": true,
     *   "smsAlertsEnabled": true,
     *   "socialMediaPlatforms": ["Facebook", "Twitter", "Instagram"],
     *   "newsletterFrequency": "WEEKLY",
     *   "reportCards": "END_OF_TERM",
     *   "parentTeacherMeetings": "ONCE_PER_TERM",
     *   "progressReports": "MID_TERM"
     * }
     * }</pre>
     *
     * <p>String values for dropdowns are validated at the DTO layer —
     * no Java enum needed since these live inside JSONB and are never
     * queried field-by-field.</p>
     */
    @Type(JsonBinaryType.class)
    @Column(name = "communication", columnDefinition = "jsonb")
    private Map<String, Object> communication;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

}
