package io.psyllabus.platform.common.seed;

import io.psyllabus.platform.auth.enums.UserStatus;
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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder  implements ApplicationRunner {
    public static final String STUDENT_ENROLLED = "[DevSeed] Student enrolled: {}";
    private final SchoolRepository schoolRepository;
    private final SchoolService schoolService;
    private final StaffService staffService;
    private final StudentService studentService;
    private final GuardianService guardianService;
    private final PlatformAdminRepository platformAdminRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedPlatformAdmin();

        if (schoolRepository.existsBySubdomainPrefix("stmary")) {
            log.info("[DevSeed] stmary tenant already exists — skipping seed");
            return;
        }

        log.info("[DevSeed] Seeding St. Mary's Academy test data...");

        try {
            // ── 1. Provision the school (public endpoint — no tenant context needed) ──
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
                    null,   // customDomain — using subdomain
                    null    // notes — internal only, not needed in seeder
            ));

            log.info("[DevSeed] School provisioned: id={}", school.schoolId());

            // ── 2. Set tenant context for all subsequent service calls ──
            TenantContext.setCurrentTenant(school.schoolId());

            // ── 3. Staff ──────────────────────────────────────────────────────────
            StaffResponse staff1 = staffService.createStaff(new StaffCreateRequest(
                    "EMP-0001",
                    "James",
                    "Mwangi",
                    "james.mwangi@stmary.ac.ke",
                    "+254711000001",
                    null,   // departmentId — department sprint pending
                    LocalDate.of(2022, 1, 10),
                    "Bachelor of Education (Mathematics)",
                    "Mathematics and Physics",
                    null    // branchId — will be set once branch is confirmed
            ));
            log.info("[DevSeed] Staff created: {}", staff1.employeeNumber());

            StaffResponse staff2 = staffService.createStaff(new StaffCreateRequest(
                    "EMP-0002",
                    "Grace",
                    "Achieng",
                    "grace.achieng@stmary.ac.ke",
                    "+254711000002",
                    null,
                    LocalDate.of(2021, 3, 15),
                    "Bachelor of Education (English)",
                    "English and Literature",
                    null
            ));
            log.info("[DevSeed] Staff created: {}", staff2.employeeNumber());

            // ── 4. Students ──────────────────────────────────────────────────────
            StudentResponse student1 = studentService.enrollStudent(new StudentCreateRequest(
                    "ADM-00001",
                    "Brian",
                    "Odhiambo",
                    LocalDate.of(2010, 5, 14),
                    Gender.MALE,
                    "brian.odhiambo@stmary.ac.ke",
                    "+254722000001",
                    null,   // branchId
                    null,   // classId — academic sprint pending
                    StudentStatus.ACTIVE,
                    null    // medicalConditions
            ));
            log.info(STUDENT_ENROLLED, student1.admissionNumber());

            StudentResponse student2 = studentService.enrollStudent(new StudentCreateRequest(
                    "ADM-00002",
                    "Amina",
                    "Hassan",
                    LocalDate.of(2011, 8, 22),
                    Gender.FEMALE,
                    "amina.hassan@stmary.ac.ke",
                    "+254722000002",
                    null,
                    null,
                    StudentStatus.ACTIVE,
                    "Mild asthma — has inhaler"
            ));
            log.info(STUDENT_ENROLLED, student2.admissionNumber());

            StudentResponse student3 = studentService.enrollStudent(new StudentCreateRequest(
                    "ADM-00003",
                    "Kevin",
                    "Kipchoge",
                    LocalDate.of(2009, 11, 3),
                    Gender.MALE,
                    "kevin.kipchoge@stmary.ac.ke",
                    "+254722000003",
                    null,
                    null,
                    StudentStatus.ACTIVE,
                    null
            ));
            log.info(STUDENT_ENROLLED, student3.admissionNumber());

            // ── 5. Guardians ─────────────────────────────────────────────────────
            GuardianResponse parent1 = guardianService.createGuardian(new GuardianCreateRequest(
                    "John",
                    "Odhiambo",
                    "0712000001",
                    "parent1@gmail.com",
                    RelationshipType.FATHER,
                    true
            ));
            log.info("[DevSeed] Guardian created: {}", parent1.email());

            GuardianResponse parent2 = guardianService.createGuardian(new GuardianCreateRequest(
                    "Fatuma",
                    "Hassan",
                    "0712000002",
                    "parent2@gmail.com",
                    RelationshipType.MOTHER,
                    true
            ));
            log.info("[DevSeed] Guardian created: {}", parent2.email());

            // ── 6. Link guardians to students ────────────────────────────────────
            // First link = primary + emergency contact automatically (see GuardianServiceImpl)
            guardianService.linkToStudent(parent1.id(), student1.id());
            log.info("[DevSeed] Linked parent1 → student1");

            guardianService.linkToStudent(parent2.id(), student2.id());
            log.info("[DevSeed] Linked parent2 → student2");

            // student3 has no guardian yet — intentional, tests empty-guardian edge case

            logCredentials(school);

        } catch (Exception e) {
            log.error("[DevSeed] Seeding failed: {}", e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }

    private void logCredentials(SchoolOnboardResponse school) {
        log.info("""
            
            ╔══════════════════════════════════════════════════════════╗
            ║              DEV SEED — ST. MARY'S ACADEMY               ║
            ║         Header for all requests: X-Tenant-ID: stmary     ║
            ╠══════════════════════════════════════════════════════════╣
            ║  Role     │ Username / Email          │ Password         ║
            ╠══════════════════════════════════════════════════════════╣
            ║  ADMIN    │ admin@stmary.ac.ke        │ Admin@123        ║
            ║  STAFF 1  │ EMP-0001                  │ EMP-0001         ║
            ║  STAFF 2  │ EMP-0002                  │ EMP-0002         ║
            ║  PARENT 1 │ parent1@gmail.com         │ 0712000001       ║
            ║  PARENT 2 │ parent2@gmail.com         │ 0712000002       ║
            ║  STUDENT 1│ ADM-00001                 │ ADM-00001        ║
            ║  STUDENT 2│ ADM-00002                 │ ADM-00002        ║
            ║  STUDENT 3│ ADM-00003                 │ ADM-00003        ║
            ╠══════════════════════════════════════════════════════════╣
            ║  School ID: {}                                           ║
            ║  Portal:    https://stmary.psyllabus.co.ke               ║
            ╚══════════════════════════════════════════════════════════╝
            """, school.schoolId());
    }

    private void seedPlatformAdmin(){
        if (platformAdminRepository.existsByEmail("platform@psyllabus.io")){
            log.info("[DevSeed] Platform admin already exists - skipping");
            return;
        }

        PlatformAdmin admin = PlatformAdmin.builder()
                .username("platform-admin")
                .email("platform@psyllabus.io")
                .hashedPassword(passwordEncoder.encode("PlatformAdmin@123"))
                .firstName("Platform")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .build();
        platformAdminRepository.save(admin);
        log.info("[DevSeed] Platform admin seeded: platform@psyllabus.io / PlatformAdmin@123");
    }
}
