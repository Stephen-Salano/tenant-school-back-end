package io.psyllabus.platform.common.seed;

import io.psyllabus.platform.academic.dto.request.AcademicTermCreateRequest;
import io.psyllabus.platform.academic.dto.request.AcademicYearCreateRequest;
import io.psyllabus.platform.academic.dto.request.ClassCreateRequest;
import io.psyllabus.platform.academic.dto.request.SubjectCategoryCreateRequest;
import io.psyllabus.platform.academic.dto.request.SubjectCreateRequest;
import io.psyllabus.platform.academic.dto.request.SubjectGroupRequest;
import io.psyllabus.platform.academic.dto.request.TeacherAssignmentCreateRequest;
import io.psyllabus.platform.academic.dto.response.AcademicTermResponse;
import io.psyllabus.platform.academic.dto.response.AcademicYearResponse;
import io.psyllabus.platform.academic.dto.response.ClassResponse;
import io.psyllabus.platform.academic.dto.response.SubjectGroupResponse;
import io.psyllabus.platform.academic.dto.response.SubjectResponse;
import io.psyllabus.platform.academic.dto.response.SubjectCategoryResponse;
import io.psyllabus.platform.academic.service.AcademicTermService;
import io.psyllabus.platform.academic.service.AcademicYearService;
import io.psyllabus.platform.academic.service.ClassService;
import io.psyllabus.platform.academic.service.EnrollmentService;
import io.psyllabus.platform.academic.service.SubjectCategoryService;
import io.psyllabus.platform.academic.service.SubjectGroupService;
import io.psyllabus.platform.academic.service.SubjectService;
import io.psyllabus.platform.academic.service.TeacherAssignmentService;
import io.psyllabus.platform.auth.enums.UserStatus;
import io.psyllabus.platform.branch.dto.request.BranchCreateRequest;
import io.psyllabus.platform.branch.dto.responses.BranchResponse;
import io.psyllabus.platform.branch.entities.Branch;
import io.psyllabus.platform.branch.repository.BranchRepository;
import io.psyllabus.platform.branch.services.BranchService;
import io.psyllabus.platform.common.annotation.SystemProcess;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.guardian.dto.request.GuardianCreateRequest;
import io.psyllabus.platform.guardian.dto.responses.GuardianResponse;
import io.psyllabus.platform.guardian.enums.RelationshipType;
import io.psyllabus.platform.guardian.services.GuardianService;
import io.psyllabus.platform.platformadmin.entities.PlatformAdmin;
import io.psyllabus.platform.platformadmin.repositories.PlatformAdminRepository;
import io.psyllabus.platform.school.dto.request.SchoolCreateRequest;
import io.psyllabus.platform.school.dto.responses.SchoolOnboardResponse;
import io.psyllabus.platform.school.repository.SchoolRepository;
import io.psyllabus.platform.school.service.SchoolService;
import io.psyllabus.platform.staff.dto.request.StaffCreateRequest;
import io.psyllabus.platform.staff.dto.responses.StaffResponse;
import io.psyllabus.platform.staff.services.StaffService;
import io.psyllabus.platform.student.dto.request.StudentCreateRequest;
import io.psyllabus.platform.student.dto.responses.StudentResponse;
import io.psyllabus.platform.student.enums.Gender;
import io.psyllabus.platform.student.enums.StudentStatus;
import io.psyllabus.platform.student.service.StudentService;
import io.psyllabus.platform.subscription.enums.PlanType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
@SystemProcess
public class DevDataSeeder implements ApplicationRunner {

    private final SchoolRepository schoolRepository;
    private final SchoolService schoolService;
    private final StaffService staffService;
    private final StudentService studentService;
    private final GuardianService guardianService;
    private final PlatformAdminRepository platformAdminRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final BranchService branchService;
    private final AcademicYearService academicYearService;
    private final AcademicTermService academicTermService;
    private final SubjectGroupService subjectGroupService;
    private final SubjectCategoryService subjectCategoryService;
    private final SubjectService subjectService;
    private final ClassService classService;
    private final TeacherAssignmentService teacherAssignmentService;
    private final EnrollmentService enrollmentService;

    @Value("${platform.dev.admin-password}")
    private String adminPassword;

    @Value("${platform.dev.school-admin-password}")
    private String schoolAdminPassword;

    @Override
    public void run(ApplicationArguments args) {
        seedPlatformAdmin();

        if (!schoolRepository.existsBySubdomainPrefix("stmary")) {
            seedStMarysAcademy();
        } else {
            log.info("[DevSeed] stmary already exists — skipping");
        }

        if (!schoolRepository.existsBySubdomainPrefix("riverside")) {
            seedRiversideAcademy();
        } else {
            log.info("[DevSeed] riverside already exists — skipping");
        }
    }

    private void seedPlatformAdmin() {
        if (platformAdminRepository.existsByEmail("platform@psyllabus.io")) {
            log.info("[DevSeed] Platform admin already exists — skipping");
            return;
        }
        PlatformAdmin admin = PlatformAdmin.builder()
                .username("platform-admin")
                .email("platform@psyllabus.io")
                .hashedPassword(passwordEncoder.encode(adminPassword))
                .firstName("Platform")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .build();
        platformAdminRepository.save(admin);
        log.info("[DevSeed] Platform admin: platform@psyllabus.io / {}", adminPassword);
    }

    // ══════════════════════════════════════════════════════════════
    //  SCHOOL A — ST. MARY'S ACADEMY (stmary)
    // ══════════════════════════════════════════════════════════════
    private void seedStMarysAcademy() {
        log.info("═══════════════════════════════════════════════════");
        log.info("  [DevSeed] Seeding St. Mary's Academy...");
        log.info("═══════════════════════════════════════════════════");

        try {
            SchoolOnboardResponse school = schoolService.onboardSchool(new SchoolCreateRequest(
                    "St. Mary's Academy",
                    "stmary",
                    "admin@stmary.ac.ke",
                    "+254700000000",
                    "Nairobi, Kenya",
                    PlanType.PROFESSIONAL,
                    "admin@stmary.ac.ke",
                    "Mary",
                    "Wanjiku",
                    "+254700000001",
                    schoolAdminPassword, null, null
            ));
            TenantContext.setCurrentTenant(school.schoolId());
            log.info("[DevSeed] Stmary schoolId={}", school.schoolId());

            // ── Branch ──
            UUID mainBranchId = getOrCreateMainBranch(school.schoolId());

            // ── Academic Years: 2025 (archived) + 2026 (current) ──
            AcademicYearResponse year2025 = academicYearService.create(new AcademicYearCreateRequest(
                    "2025", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 15)
            ));
            AcademicYearResponse year2026 = academicYearService.create(new AcademicYearCreateRequest(
                    "2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
            ));
            academicYearService.setCurrentYear(year2026.id());
            log.info("[DevSeed] Academic years created: {} (archived), {} (current)", year2025.id(), year2026.id());

            // ── Terms for 2025 ──
            List<AcademicTermResponse> terms2025 = List.of(
                    academicTermService.create(new AcademicTermCreateRequest(
                            year2025.id(), "Term 1", 1, LocalDate.of(2025, 1, 6), LocalDate.of(2025, 4, 4))),
                    academicTermService.create(new AcademicTermCreateRequest(
                            year2025.id(), "Term 2", 2, LocalDate.of(2025, 5, 5), LocalDate.of(2025, 8, 1))),
                    academicTermService.create(new AcademicTermCreateRequest(
                            year2025.id(), "Term 3", 3, LocalDate.of(2025, 9, 1), LocalDate.of(2025, 11, 28)))
            );

            // ── Terms for 2026 ──
            List<AcademicTermResponse> terms2026 = List.of(
                    academicTermService.create(new AcademicTermCreateRequest(
                            year2026.id(), "Term 1", 1, LocalDate.of(2026, 1, 5), LocalDate.of(2026, 4, 10))),
                    academicTermService.create(new AcademicTermCreateRequest(
                            year2026.id(), "Term 2", 2, LocalDate.of(2026, 5, 4), LocalDate.of(2026, 8, 7))),
                    academicTermService.create(new AcademicTermCreateRequest(
                            year2026.id(), "Term 3", 3, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 20)))
            );
            academicTermService.setCurrentTerm(terms2026.get(0).id());
            log.info("[DevSeed] Terms created: 2025 ({}), 2026 ({})", terms2025.size(), terms2026.size());

            // ── Subject Groups ──
            SubjectGroupResponse mathSci = subjectGroupService.create(
                    new SubjectGroupRequest("Mathematics & Sciences", "STEM subjects"));
            SubjectGroupResponse lang = subjectGroupService.create(
                    new SubjectGroupRequest("Languages", "Language subjects"));
            SubjectGroupResponse humanities = subjectGroupService.create(
                    new SubjectGroupRequest("Humanities", "Social studies and arts"));

            // ── Subject Categories ──
            SubjectCategoryResponse coreCat = subjectCategoryService.createCategory(
                    new SubjectCategoryCreateRequest("Core", "Mandatory subjects"));
            SubjectCategoryResponse electiveCat = subjectCategoryService.createCategory(
                    new SubjectCategoryCreateRequest("Elective", "Optional subjects"));

            // ── 10 Subjects ──
            List<SubjectResponse> subjects = List.of(
                    subjectService.createSubject(new SubjectCreateRequest(
                            "Mathematics", "MATH", mathSci.id(), coreCat.id(), 100, 50)),
                    subjectService.createSubject(new SubjectCreateRequest(
                            "English", "ENG", lang.id(), coreCat.id(), 100, 45)),
                    subjectService.createSubject(new SubjectCreateRequest(
                            "Kiswahili", "KIS", lang.id(), coreCat.id(), 100, 45)),
                    subjectService.createSubject(new SubjectCreateRequest(
                            "Science", "SCI", mathSci.id(), coreCat.id(), 100, 50)),
                    subjectService.createSubject(new SubjectCreateRequest(
                            "Social Studies", "SOS", humanities.id(), coreCat.id(), 100, 40)),
                    subjectService.createSubject(new SubjectCreateRequest(
                            "Religious Education", "CRE", humanities.id(), coreCat.id(), 100, 40)),
                    subjectService.createSubject(new SubjectCreateRequest(
                            "Art & Craft", "ART", humanities.id(), electiveCat.id(), 100, 40)),
                    subjectService.createSubject(new SubjectCreateRequest(
                            "Physical Education", "PE", mathSci.id(), electiveCat.id(), 100, 40)),
                    subjectService.createSubject(new SubjectCreateRequest(
                            "Music", "MUS", humanities.id(), electiveCat.id(), 100, 40)),
                    subjectService.createSubject(new SubjectCreateRequest(
                            "Home Science", "HSC", mathSci.id(), electiveCat.id(), 100, 40))
            );
            log.info("[DevSeed] Created {} subjects", subjects.size());

            // ── 6 Classes: Grade 1–3 × East/West ──
            List<ClassResponse> classes = new ArrayList<>();
            for (String grade : List.of("Grade 1", "Grade 2", "Grade 3")) {
                for (String stream : List.of("East", "West")) {
                    ClassResponse cls = classService.createClass(new ClassCreateRequest(
                            grade + " " + stream, grade, stream, year2026.id(), mainBranchId, 40));
                    classes.add(cls);
                }
            }
            log.info("[DevSeed] Created {} classes", classes.size());

            // ── 8 Teachers ──
            List<StaffResponse> teachers = List.of(
                    staffService.createStaff(new StaffCreateRequest(
                            "EMP-0001", "James", "Mwangi", "james.mwangi@stmary.ac.ke", "+254711000001",
                            null, LocalDate.of(2022, 1, 10), "B.Ed Mathematics", "Mathematics", mainBranchId)),
                    staffService.createStaff(new StaffCreateRequest(
                            "EMP-0002", "Grace", "Achieng", "grace.achieng@stmary.ac.ke", "+254711000002",
                            null, LocalDate.of(2021, 3, 15), "B.Ed English", "English", mainBranchId)),
                    staffService.createStaff(new StaffCreateRequest(
                            "EMP-0003", "Peter", "Otieno", "peter.otieno@stmary.ac.ke", "+254711000003",
                            null, LocalDate.of(2020, 6, 1), "B.Ed Sciences", "Science", mainBranchId)),
                    staffService.createStaff(new StaffCreateRequest(
                            "EMP-0004", "Susan", "Njoroge", "susan.njoroge@stmary.ac.ke", "+254711000004",
                            null, LocalDate.of(2019, 9, 1), "B.Ed Kiswahili", "Kiswahili", mainBranchId)),
                    staffService.createStaff(new StaffCreateRequest(
                            "EMP-0005", "David", "Kimani", "david.kimani@stmary.ac.ke", "+254711000005",
                            null, LocalDate.of(2023, 1, 15), "B.Ed Social Studies", "Social Studies", mainBranchId)),
                    staffService.createStaff(new StaffCreateRequest(
                            "EMP-0006", "Faith", "Wanjiru", "faith.wanjiru@stmary.ac.ke", "+254711000006",
                            null, LocalDate.of(2022, 8, 1), "B.Ed CRE", "Religious Education", mainBranchId)),
                    staffService.createStaff(new StaffCreateRequest(
                            "EMP-0007", "Michael", "Ndegwa", "michael.ndegwa@stmary.ac.ke", "+254711000007",
                            null, LocalDate.of(2021, 2, 1), "B.Ed Art", "Art & Craft", mainBranchId)),
                    staffService.createStaff(new StaffCreateRequest(
                            "EMP-0008", "Rose", "Muthoni", "rose.muthoni@stmary.ac.ke", "+254711000008",
                            null, LocalDate.of(2020, 4, 1), "B.Ed PE", "Physical Education", mainBranchId))
            );
            log.info("[DevSeed] Created {} staff members", teachers.size());

            // ── Teacher Assignments: each teacher → Grade 1 East + Term 1 (current) ──
            // Grade 1 East = classes.get(0), Term 1 2026 = terms2026.get(0)
            ClassResponse grade1East = classes.get(0);
            for (int i = 0; i < teachers.size(); i++) {
                UUID subjectId = subjects.get(i % subjects.size()).id();
                teacherAssignmentService.assignTeacher(new TeacherAssignmentCreateRequest(
                        teachers.get(i).id(), grade1East.id(), subjectId,
                        terms2026.get(0).id(), i == 0, 6 + (i % 3), null));
            }
            log.info("[DevSeed] Teacher assignments created for {} teachers in Grade 1 East", teachers.size());

            // ── 12 Students ──
            List<StudentResponse> students = List.of(
                    studentService.enrollStudent(new StudentCreateRequest(
                            "ADM-00001", "Brian", "Odhiambo", LocalDate.of(2010, 5, 14), Gender.MALE,
                            "brian.odhiambo@stmary.ac.ke", "+254722000001", mainBranchId, null, StudentStatus.ACTIVE, null)),
                    studentService.enrollStudent(new StudentCreateRequest(
                            "ADM-00002", "Amina", "Hassan", LocalDate.of(2011, 8, 22), Gender.FEMALE,
                            "amina.hassan@stmary.ac.ke", "+254722000002", mainBranchId, null, StudentStatus.ACTIVE, null)),
                    studentService.enrollStudent(new StudentCreateRequest(
                            "ADM-00003", "Kevin", "Mutiso", LocalDate.of(2010, 3, 10), Gender.MALE,
                            "kevin.mutiso@stmary.ac.ke", "+254722000003", mainBranchId, null, StudentStatus.ACTIVE, null)),
                    studentService.enrollStudent(new StudentCreateRequest(
                            "ADM-00004", "Diana", "Kariuki", LocalDate.of(2011, 11, 5), Gender.FEMALE,
                            "diana.kariuki@stmary.ac.ke", "+254722000004", mainBranchId, null, StudentStatus.ACTIVE, null)),
                    studentService.enrollStudent(new StudentCreateRequest(
                            "ADM-00005", "Eric", "Kamau", LocalDate.of(2010, 7, 19), Gender.MALE,
                            "eric.kamau@stmary.ac.ke", "+254722000005", mainBranchId, null, StudentStatus.ACTIVE, null)),
                    studentService.enrollStudent(new StudentCreateRequest(
                            "ADM-00006", "Fatima", "Ali", LocalDate.of(2011, 2, 28), Gender.FEMALE,
                            "fatima.ali@stmary.ac.ke", "+254722000006", mainBranchId, null, StudentStatus.ACTIVE, null)),
                    studentService.enrollStudent(new StudentCreateRequest(
                            "ADM-00007", "Samuel", "Ochieng", LocalDate.of(2010, 9, 3), Gender.MALE,
                            "samuel.ochieng@stmary.ac.ke", "+254722000007", mainBranchId, null, StudentStatus.ACTIVE, null)),
                    studentService.enrollStudent(new StudentCreateRequest(
                            "ADM-00008", "Lilian", "Njeri", LocalDate.of(2011, 4, 15), Gender.FEMALE,
                            "lilian.njeri@stmary.ac.ke", "+254722000008", mainBranchId, null, StudentStatus.ACTIVE, null)),
                    studentService.enrollStudent(new StudentCreateRequest(
                            "ADM-00009", "Thomas", "Wambua", LocalDate.of(2010, 12, 1), Gender.MALE,
                            "thomas.wambua@stmary.ac.ke", "+254722000009", mainBranchId, null, StudentStatus.ACTIVE, null)),
                    studentService.enrollStudent(new StudentCreateRequest(
                            "ADM-00010", "Grace", "Mwende", LocalDate.of(2011, 6, 20), Gender.FEMALE,
                            "grace.mwende@stmary.ac.ke", "+254722000010", mainBranchId, null, StudentStatus.ACTIVE, null)),
                    studentService.enrollStudent(new StudentCreateRequest(
                            "ADM-00011", "Daniel", "Njoroge", LocalDate.of(2010, 1, 30), Gender.MALE,
                            "daniel.njoroge@stmary.ac.ke", "+254722000011", mainBranchId, null, StudentStatus.ACTIVE, null)),
                    studentService.enrollStudent(new StudentCreateRequest(
                            "ADM-00012", "Emily", "Wanjiru", LocalDate.of(2011, 10, 8), Gender.FEMALE,
                            "emily.wanjiru@stmary.ac.ke", "+254722000012", mainBranchId, null, StudentStatus.ACTIVE, null))
            );
            log.info("[DevSeed] Created {} students", students.size());

            // ── Enroll first 6 students in Grade 1 East, rest in Grade 1 West ──
            enrollmentService.enrollStudent(grade1East.id(), students.get(0).id());
            enrollmentService.enrollStudent(grade1East.id(), students.get(1).id());
            enrollmentService.enrollStudent(grade1East.id(), students.get(2).id());
            enrollmentService.enrollStudent(grade1East.id(), students.get(3).id());
            enrollmentService.enrollStudent(grade1East.id(), students.get(4).id());
            enrollmentService.enrollStudent(grade1East.id(), students.get(5).id());

            ClassResponse grade1West = classes.get(1);
            enrollmentService.enrollStudent(grade1West.id(), students.get(6).id());
            enrollmentService.enrollStudent(grade1West.id(), students.get(7).id());
            enrollmentService.enrollStudent(grade1West.id(), students.get(8).id());
            enrollmentService.enrollStudent(grade1West.id(), students.get(9).id());
            enrollmentService.enrollStudent(grade1West.id(), students.get(10).id());
            enrollmentService.enrollStudent(grade1West.id(), students.get(11).id());
            log.info("[DevSeed] Students enrolled in Grade 1 East (6) and Grade 1 West (6)");

            // ── 8 Guardians ──
            List<GuardianResponse> guardians = new ArrayList<>();
            guardians.add(guardianService.createGuardian(new GuardianCreateRequest(
                    "John", "Odhiambo", "0712000001", "john.odhiambo@gmail.com", RelationshipType.FATHER, true)));
            guardians.add(guardianService.createGuardian(new GuardianCreateRequest(
                    "Mary", "Hassan", "0712000002", "mary.hassan@gmail.com", RelationshipType.MOTHER, true)));
            guardians.add(guardianService.createGuardian(new GuardianCreateRequest(
                    "Peter", "Mutiso", "0712000003", "peter.mutiso@gmail.com", RelationshipType.FATHER, true)));
            guardians.add(guardianService.createGuardian(new GuardianCreateRequest(
                    "Grace", "Kariuki", "0712000004", "grace.kariuki@gmail.com", RelationshipType.MOTHER, true)));
            guardians.add(guardianService.createGuardian(new GuardianCreateRequest(
                    "Samuel", "Kamau", "0712000005", "samuel.kamau@gmail.com", RelationshipType.FATHER, true)));
            guardians.add(guardianService.createGuardian(new GuardianCreateRequest(
                    "Amina", "Ali", "0712000006", "amina.ali@gmail.com", RelationshipType.MOTHER, true)));
            guardians.add(guardianService.createGuardian(new GuardianCreateRequest(
                    "James", "Ochieng", "0712000007", "james.ochieng@gmail.com", RelationshipType.FATHER, true)));
            guardians.add(guardianService.createGuardian(new GuardianCreateRequest(
                    "Sarah", "Njeri", "0712000008", "sarah.njeri@gmail.com", RelationshipType.MOTHER, true)));

            // Link first 4 guardians to first 4 students (one-to-one), last 4 guardians to students 5-8, last 4 students unlinked
            guardianService.linkToStudent(guardians.get(0).id(), students.get(0).id());
            guardianService.linkToStudent(guardians.get(1).id(), students.get(1).id());
            guardianService.linkToStudent(guardians.get(2).id(), students.get(2).id());
            guardianService.linkToStudent(guardians.get(3).id(), students.get(3).id());
            guardianService.linkToStudent(guardians.get(4).id(), students.get(4).id());
            guardianService.linkToStudent(guardians.get(5).id(), students.get(5).id());
            guardianService.linkToStudent(guardians.get(6).id(), students.get(6).id());
            guardianService.linkToStudent(guardians.get(7).id(), students.get(7).id());
            log.info("[DevSeed] Created and linked {} guardians", guardians.size());

            logStMarysCredentials(school);

        } catch (Exception e) {
            log.error("[DevSeed] St. Mary's seed FAILED: {}", e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  SCHOOL B — RIVERSIDE ACADEMY (riverside)
    // ══════════════════════════════════════════════════════════════
    private void seedRiversideAcademy() {
        log.info("═══════════════════════════════════════════════════");
        log.info("  [DevSeed] Seeding Riverside Academy...");
        log.info("═══════════════════════════════════════════════════");

        try {
            SchoolOnboardResponse school = schoolService.onboardSchool(new SchoolCreateRequest(
                    "Riverside Academy",
                    "riverside",
                    "admin@riverside.ac.ke",
                    "+254720000000",
                    "Kisumu, Kenya",
                    PlanType.BASIC,
                    "admin@riverside.ac.ke",
                    "Grace",
                    "Auma",
                    "+254720000001",
                    schoolAdminPassword, null, null
            ));
            TenantContext.setCurrentTenant(school.schoolId());
            log.info("[DevSeed] Riverside schoolId={}", school.schoolId());

            // ── Branch ──
            UUID mainBranchId = getOrCreateMainBranch(school.schoolId());

            // ── Academic Year 2026 (current only) ──
            AcademicYearResponse year2026 = academicYearService.create(new AcademicYearCreateRequest(
                    "2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
            ));
            academicYearService.setCurrentYear(year2026.id());

            List<AcademicTermResponse> terms2026 = List.of(
                    academicTermService.create(new AcademicTermCreateRequest(
                            year2026.id(), "Term 1", 1, LocalDate.of(2026, 1, 5), LocalDate.of(2026, 4, 10))),
                    academicTermService.create(new AcademicTermCreateRequest(
                            year2026.id(), "Term 2", 2, LocalDate.of(2026, 5, 4), LocalDate.of(2026, 8, 7))),
                    academicTermService.create(new AcademicTermCreateRequest(
                            year2026.id(), "Term 3", 3, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 20)))
            );
            academicTermService.setCurrentTerm(terms2026.get(0).id());
            log.info("[DevSeed] 2026 year + {} terms created", terms2026.size());

            // ── Subject Groups ──
            SubjectGroupResponse coreGroup = subjectGroupService.create(
                    new SubjectGroupRequest("Core Subjects", "Mandatory curriculum"));
            SubjectGroupResponse creativeGroup = subjectGroupService.create(
                    new SubjectGroupRequest("Creative Arts", "Art and music subjects"));

            // ── Subject Categories ──
            SubjectCategoryResponse coreCat = subjectCategoryService.createCategory(
                    new SubjectCategoryCreateRequest("Core", "Mandatory"));
            SubjectCategoryResponse electiveCat = subjectCategoryService.createCategory(
                    new SubjectCategoryCreateRequest("Elective", "Optional"));

            // ── 8 Subjects ──
            List<SubjectResponse> subjects = List.of(
                    subjectService.createSubject(new SubjectCreateRequest(
                            "Mathematics", "MATH", coreGroup.id(), coreCat.id(), 100, 50)),
                    subjectService.createSubject(new SubjectCreateRequest(
                            "English", "ENG", coreGroup.id(), coreCat.id(), 100, 45)),
                    subjectService.createSubject(new SubjectCreateRequest(
                            "Kiswahili", "KIS", coreGroup.id(), coreCat.id(), 100, 45)),
                    subjectService.createSubject(new SubjectCreateRequest(
                            "Science", "SCI", coreGroup.id(), coreCat.id(), 100, 50)),
                    subjectService.createSubject(new SubjectCreateRequest(
                            "Social Studies", "SOS", coreGroup.id(), coreCat.id(), 100, 40)),
                    subjectService.createSubject(new SubjectCreateRequest(
                            "CRE", "CRE", coreGroup.id(), coreCat.id(), 100, 40)),
                    subjectService.createSubject(new SubjectCreateRequest(
                            "Art & Craft", "ART", creativeGroup.id(), electiveCat.id(), 100, 40)),
                    subjectService.createSubject(new SubjectCreateRequest(
                            "Music", "MUS", creativeGroup.id(), electiveCat.id(), 100, 40))
            );
            log.info("[DevSeed] Created {} subjects", subjects.size());

            // ── 4 Classes: Grade 1–2 × East/West ──
            List<ClassResponse> classes = new ArrayList<>();
            for (String grade : List.of("Grade 1", "Grade 2")) {
                for (String stream : List.of("East", "West")) {
                    ClassResponse cls = classService.createClass(new ClassCreateRequest(
                            grade + " " + stream, grade, stream, year2026.id(), mainBranchId, 35));
                    classes.add(cls);
                }
            }
            log.info("[DevSeed] Created {} classes", classes.size());

            // ── 6 Teachers ──
            List<StaffResponse> teachers = List.of(
                    staffService.createStaff(new StaffCreateRequest(
                            "EMP-R001", "Vincent", "Oduor", "vincent.oduoor@riverside.ac.ke", "+254731000001",
                            null, LocalDate.of(2021, 1, 10), "B.Ed Maths", "Mathematics", mainBranchId)),
                    staffService.createStaff(new StaffCreateRequest(
                            "EMP-R002", "Alice", "Awino", "alice.awino@riverside.ac.ke", "+254731000002",
                            null, LocalDate.of(2020, 3, 15), "B.Ed English", "English", mainBranchId)),
                    staffService.createStaff(new StaffCreateRequest(
                            "EMP-R003", "Ben", "Otieno", "ben.otieno@riverside.ac.ke", "+254731000003",
                            null, LocalDate.of(2019, 6, 1), "B.Ed Sciences", "Science", mainBranchId)),
                    staffService.createStaff(new StaffCreateRequest(
                            "EMP-R004", "Catherine", "Achieng", "catherine.achieng@riverside.ac.ke", "+254731000004",
                            null, LocalDate.of(2022, 9, 1), "B.Ed Kiswahili", "Kiswahili", mainBranchId)),
                    staffService.createStaff(new StaffCreateRequest(
                            "EMP-R005", "Francis", "Okoth", "francis.okoth@riverside.ac.ke", "+254731000005",
                            null, LocalDate.of(2021, 2, 1), "B.Ed Social", "Social Studies", mainBranchId)),
                    staffService.createStaff(new StaffCreateRequest(
                            "EMP-R006", "Pamela", "Anyango", "pamela.anyango@riverside.ac.ke", "+254731000006",
                            null, LocalDate.of(2023, 4, 1), "B.Ed Art", "Art & Craft", mainBranchId))
            );
            log.info("[DevSeed] Created {} staff members", teachers.size());

            // ── Teacher Assignments: each teacher → Grade 1 East + Term 1 ──
            ClassResponse grade1East = classes.get(0);
            for (int i = 0; i < teachers.size(); i++) {
                UUID subjectId = subjects.get(i % subjects.size()).id();
                teacherAssignmentService.assignTeacher(new TeacherAssignmentCreateRequest(
                        teachers.get(i).id(), grade1East.id(), subjectId,
                        terms2026.get(0).id(), i == 0, 6 + (i % 3), null));
            }
            log.info("[DevSeed] Teacher assignments created for {} teachers in Grade 1 East", teachers.size());

            // ── 10 Students ──
            List<StudentResponse> students = List.of(
                    studentService.enrollStudent(new StudentCreateRequest(
                            "ADM-R0001", "Ochieng", "Apondi", LocalDate.of(2011, 5, 14), Gender.MALE,
                            "ochieng.apondi@riverside.ac.ke", "+254742000001", mainBranchId, null, StudentStatus.ACTIVE, null)),
                    studentService.enrollStudent(new StudentCreateRequest(
                            "ADM-R0002", "Auma", "Grace", LocalDate.of(2012, 8, 22), Gender.FEMALE,
                            "auma.grace@riverside.ac.ke", "+254742000002", mainBranchId, null, StudentStatus.ACTIVE, null)),
                    studentService.enrollStudent(new StudentCreateRequest(
                            "ADM-R0003", "Odhiambo", "Brian", LocalDate.of(2011, 3, 10), Gender.MALE,
                            "odhiambo.brian@riverside.ac.ke", "+254742000003", mainBranchId, null, StudentStatus.ACTIVE, null)),
                    studentService.enrollStudent(new StudentCreateRequest(
                            "ADM-R0004", "Anyango", "Faith", LocalDate.of(2012, 11, 5), Gender.FEMALE,
                            "anyango.faith@riverside.ac.ke", "+254742000004", mainBranchId, null, StudentStatus.ACTIVE, null)),
                    studentService.enrollStudent(new StudentCreateRequest(
                            "ADM-R0005", "Owuor", "Kevin", LocalDate.of(2011, 7, 19), Gender.MALE,
                            "owuor.kevin@riverside.ac.ke", "+254742000005", mainBranchId, null, StudentStatus.ACTIVE, null)),
                    studentService.enrollStudent(new StudentCreateRequest(
                            "ADM-R0006", "Adhiambo", "Diana", LocalDate.of(2012, 2, 28), Gender.FEMALE,
                            "adhiambo.diana@riverside.ac.ke", "+254742000006", mainBranchId, null, StudentStatus.ACTIVE, null)),
                    studentService.enrollStudent(new StudentCreateRequest(
                            "ADM-R0007", "Ombogo", "Samuel", LocalDate.of(2011, 9, 3), Gender.MALE,
                            "ombogo.samuel@riverside.ac.ke", "+254742000007", mainBranchId, null, StudentStatus.ACTIVE, null)),
                    studentService.enrollStudent(new StudentCreateRequest(
                            "ADM-R0008", "Otieno", "Lilian", LocalDate.of(2012, 4, 15), Gender.FEMALE,
                            "otieno.lilian@riverside.ac.ke", "+254742000008", mainBranchId, null, StudentStatus.ACTIVE, null)),
                    studentService.enrollStudent(new StudentCreateRequest(
                            "ADM-R0009", "Ochieng", "Thomas", LocalDate.of(2011, 12, 1), Gender.MALE,
                            "ochieng.thomas@riverside.ac.ke", "+254742000009", mainBranchId, null, StudentStatus.ACTIVE, null)),
                    studentService.enrollStudent(new StudentCreateRequest(
                            "ADM-R0010", "Awino", "Grace", LocalDate.of(2012, 6, 20), Gender.FEMALE,
                            "awino.grace@riverside.ac.ke", "+254742000010", mainBranchId, null, StudentStatus.ACTIVE, null))
            );
            log.info("[DevSeed] Created {} students", students.size());

            // ── Enroll students: 5 in Grade 1 East, 5 in Grade 1 West ──
            enrollmentService.enrollStudent(grade1East.id(), students.get(0).id());
            enrollmentService.enrollStudent(grade1East.id(), students.get(1).id());
            enrollmentService.enrollStudent(grade1East.id(), students.get(2).id());
            enrollmentService.enrollStudent(grade1East.id(), students.get(3).id());
            enrollmentService.enrollStudent(grade1East.id(), students.get(4).id());

            ClassResponse grade1West = classes.get(1);
            enrollmentService.enrollStudent(grade1West.id(), students.get(5).id());
            enrollmentService.enrollStudent(grade1West.id(), students.get(6).id());
            enrollmentService.enrollStudent(grade1West.id(), students.get(7).id());
            enrollmentService.enrollStudent(grade1West.id(), students.get(8).id());
            enrollmentService.enrollStudent(grade1West.id(), students.get(9).id());
            log.info("[DevSeed] Students enrolled in Grade 1 East (5) and Grade 1 West (5)");

            // ── 6 Guardians ──
            List<GuardianResponse> guardians = List.of(
                    guardianService.createGuardian(new GuardianCreateRequest(
                            "James", "Apondi", "0713000001", "james.apondi@gmail.com", RelationshipType.FATHER, true)),
                    guardianService.createGuardian(new GuardianCreateRequest(
                            "Mary", "Auma", "0713000002", "mary.auma@gmail.com", RelationshipType.MOTHER, true)),
                    guardianService.createGuardian(new GuardianCreateRequest(
                            "Peter", "Odhiambo", "0713000003", "peter.odhiambo@gmail.com", RelationshipType.FATHER, true)),
                    guardianService.createGuardian(new GuardianCreateRequest(
                            "Grace", "Anyango", "0713000004", "grace.anyango@gmail.com", RelationshipType.MOTHER, true)),
                    guardianService.createGuardian(new GuardianCreateRequest(
                            "Samuel", "Owuor", "0713000005", "samuel.owuor@gmail.com", RelationshipType.FATHER, true)),
                    guardianService.createGuardian(new GuardianCreateRequest(
                            "Catherine", "Adhiambo", "0713000006", "catherine.adhiambo@gmail.com", RelationshipType.MOTHER, true))
            );

            // Link all 6 guardians to first 6 students
            guardianService.linkToStudent(guardians.get(0).id(), students.get(0).id());
            guardianService.linkToStudent(guardians.get(1).id(), students.get(1).id());
            guardianService.linkToStudent(guardians.get(2).id(), students.get(2).id());
            guardianService.linkToStudent(guardians.get(3).id(), students.get(3).id());
            guardianService.linkToStudent(guardians.get(4).id(), students.get(4).id());
            guardianService.linkToStudent(guardians.get(5).id(), students.get(5).id());
            log.info("[DevSeed] Created and linked {} guardians", guardians.size());

            logRiversideCredentials(school);

        } catch (Exception e) {
            log.error("[DevSeed] Riverside seed FAILED: {}", e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  SHARED HELPERS
    // ══════════════════════════════════════════════════════════════

    private UUID getOrCreateMainBranch(UUID tenantId) {
        return branchRepository.findByCodeAndTenantId("MAIN", tenantId)
                .map(Branch::getId)
                .orElseGet(() -> {
                    BranchResponse created = branchService.createBranch(new BranchCreateRequest(
                            "Main Campus", "MAIN", "Main Campus Address",
                            "+254700000100", "main@school.ac.ke",
                            "School", "Admin", "+254700000101"));
                    return created.id();
                });
    }

    private void logStMarysCredentials(SchoolOnboardResponse school) {
        log.info("""
                ╔════════════════════════════════════════════════════════════════╗
                ║         DEV SEED — ST. MARY'S ACADEMY (stmary)                  ║
                ║  Header: X-Tenant-ID: stmary  |  Auth: admission number        ║
                ╠════════════════════════════════════════════════════════════════╣
                ║  Role       │ Username / Email           │ Password              ║
                ╠════════════════════════════════════════════════════════════════╣
                ║  ADMIN      │ admin@stmary.ac.ke         │ Admin@123             ║
                ║  TEACHER 1  │ EMP-0001                   │ EMP-0001              ║
                ║  TEACHER 2  │ EMP-0002                   │ EMP-0002              ║
                ║  TEACHER 3  │ EMP-0003                   │ EMP-0003              ║
                ║  TEACHER 4  │ EMP-0004                   │ EMP-0004              ║
                ║  TEACHER 5  │ EMP-0005                   │ EMP-0005              ║
                ║  TEACHER 6  │ EMP-0006                   │ EMP-0006              ║
                ║  TEACHER 7  │ EMP-0007                   │ EMP-0007              ║
                ║  TEACHER 8  │ EMP-0008                   │ EMP-0008              ║
                ║  STUDENT 1  │ ADM-00001                  │ ADM-00001             ║
                ║  STUDENT 2  │ ADM-00002                  │ ADM-00002             ║
                ║  STUDENT 3  │ ADM-00003                  │ ADM-00003             ║
                ║  STUDENT 4  │ ADM-00004                  │ ADM-00004             ║
                ║  STUDENT 5  │ ADM-00005                  │ ADM-00005             ║
                ║  STUDENT 6  │ ADM-00006                  │ ADM-00006             ║
                ║  STUDENT 7  │ ADM-00007                  │ ADM-00007             ║
                ║  STUDENT 8  │ ADM-00008                  │ ADM-00008             ║
                ║  STUDENT 9  │ ADM-00009                  │ ADM-00009             ║
                ║  STUDENT 10 │ ADM-00010                  │ ADM-00010             ║
                ║  STUDENT 11 │ ADM-00011                  │ ADM-00011             ║
                ║  STUDENT 12 │ ADM-00012                  │ ADM-00012             ║
                ║  PARENT 1   │ john.odhiambo@gmail.com    │ 0712000001            ║
                ║  PARENT 2   │ mary.hassan@gmail.com      │ 0712000002            ║
                ╠════════════════════════════════════════════════════════════════╣
                ║  School ID: {}                                             ║
                ║  Academic: 2025 (archived), 2026 (current), 3 terms/year  ║
                ║  Classes: 6 (Grade 1-3, East/West)  |  Subjects: 10       ║
                ╚════════════════════════════════════════════════════════════════╝
                """, school.schoolId());
    }

    private void logRiversideCredentials(SchoolOnboardResponse school) {
        log.info("""
                ╔════════════════════════════════════════════════════════════════╗
                ║         DEV SEED — RIVERSIDE ACADEMY (riverside)               ║
                ║  Header: X-Tenant-ID: riverside  |  Auth: email vanity domain   ║
                ╠════════════════════════════════════════════════════════════════╣
                ║  Role       │ Username / Email             │ Password            ║
                ╠════════════════════════════════════════════════════════════════╣
                ║  ADMIN      │ admin@riverside.ac.ke        │ Admin@123           ║
                ║  TEACHER 1  │ EMP-R001                     │ EMP-R001            ║
                ║  TEACHER 2  │ EMP-R002                     │ EMP-R002            ║
                ║  TEACHER 3  │ EMP-R003                     │ EMP-R003            ║
                ║  TEACHER 4  │ EMP-R004                     │ EMP-R004            ║
                ║  TEACHER 5  │ EMP-R005                     │ EMP-R005            ║
                ║  TEACHER 6  │ EMP-R006                     │ EMP-R006            ║
                ║  STUDENT 1  │ ADM-R0001                     │ ADM-R0001           ║
                ║  STUDENT 2  │ ADM-R0002                     │ ADM-R0002           ║
                ║  STUDENT 3  │ ADM-R0003                     │ ADM-R0003           ║
                ║  STUDENT 4  │ ADM-R0004                     │ ADM-R0004           ║
                ║  STUDENT 5  │ ADM-R0005                     │ ADM-R0005           ║
                ║  STUDENT 6  │ ADM-R0006                     │ ADM-R0006           ║
                ║  STUDENT 7  │ ADM-R0007                     │ ADM-R0007           ║
                ║  STUDENT 8  │ ADM-R0008                     │ ADM-R0008           ║
                ║  STUDENT 9  │ ADM-R0009                     │ ADM-R0009           ║
                ║  STUDENT 10 │ ADM-R0010                     │ ADM-R0010           ║
                ║  PARENT 1   │ james.apondi@gmail.com       │ 0713000001          ║
                ║  PARENT 2   │ mary.auma@gmail.com          │ 0713000002          ║
                ╠════════════════════════════════════════════════════════════════╣
                ║  School ID: {}                                             ║
                ║  Academic: 2026 (current), 3 terms  |  Classes: 4 (Grade 1-2) ║
                ║  Subjects: 8  |  Plan: BASIC                                   ║
                ╚════════════════════════════════════════════════════════════════╝
                """, school.schoolId());
    }
}