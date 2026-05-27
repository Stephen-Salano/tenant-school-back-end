package io.psyllabus.platform.academic.repository;

import io.psyllabus.platform.academic.dto.response.TeacherAssignmentResponse;
import io.psyllabus.platform.academic.entities.*;
import io.psyllabus.platform.academic.enums.AcademicStatus;
import io.psyllabus.platform.academic.enums.AssignmentStatus;
import io.psyllabus.platform.staff.entities.Staff;
import io.psyllabus.platform.staff.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TeacherAssignmentRepositoryTest {

    @Autowired private TeacherAssignmentRepository teacherAssignmentRepository;
    @Autowired private StaffRepository staffRepository;
    @Autowired private SchoolClassRepository schoolClassRepository;
    @Autowired private AcademicYearRepository academicYearRepository;
    @Autowired private AcademicTermRepository academicTermRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private StudentClassEnrollmentRepository enrollmentRepository;

    @Test
    void findResponsesByStaffId_ShouldReturnEnrichedData() {
        UUID tenantId = UUID.randomUUID();

        // 1. Setup Academic Year
        AcademicYear year = AcademicYear.builder()
                .tenantId(tenantId)
                .name("2026")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .build();
        year.setCreatedAt(java.time.LocalDateTime.now());
        year.setUpdatedAt(java.time.LocalDateTime.now());
        year = academicYearRepository.save(year);

        // 2. Setup Academic Term
        AcademicTerm term = AcademicTerm.builder()
                .tenantId(tenantId)
                .academicYearId(year.getId())
                .name("Term 1")
                .termNumber(1)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 4, 30))
                .build();
        term.setCreatedAt(java.time.LocalDateTime.now());
        term.setUpdatedAt(java.time.LocalDateTime.now());
        term = academicTermRepository.save(term);

        // 3. Setup Class
        SchoolClass schoolClass = SchoolClass.builder()
                .tenantId(tenantId)
                .name("Grade 1 East")
                .academicYearId(year.getId())
                .status(AcademicStatus.ACTIVE)
                .build();
        schoolClass.setCreatedAt(java.time.LocalDateTime.now());
        schoolClass.setUpdatedAt(java.time.LocalDateTime.now());
        schoolClass = schoolClassRepository.save(schoolClass);

        // 4. Setup Subject
        Subject subject = Subject.builder()
                .tenantId(tenantId)
                .name("Mathematics")
                .code("MATH-G1")
                .build();
        subject.setCreatedAt(java.time.LocalDateTime.now());
        subject.setUpdatedAt(java.time.LocalDateTime.now());
        subject = subjectRepository.save(subject);

        // 5. Setup Staff
        Staff staff = Staff.builder()
                .tenantId(tenantId)
                .userId(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .employeeNumber("EMP001")
                .hireDate(LocalDate.now())
                .build();
        staff.setCreatedAt(java.time.LocalDateTime.now());
        staff.setUpdatedAt(java.time.LocalDateTime.now());
        staff = staffRepository.save(staff);

        // 6. Setup Teacher Assignment
        TeacherAssignment assignment = TeacherAssignment.builder()
                .tenantId(tenantId)
                .staffId(staff.getId())
                .classId(schoolClass.getId())
                .subjectId(subject.getId())
                .academicTermId(term.getId())
                .status(AssignmentStatus.ACTIVE)
                .build();
        assignment.setCreatedAt(java.time.LocalDateTime.now());
        assignment.setUpdatedAt(java.time.LocalDateTime.now());
        teacherAssignmentRepository.save(assignment);

        // 7. Setup Enrollments
        StudentClassEnrollment enrollment1 = StudentClassEnrollment.builder()
                .tenantId(tenantId)
                .studentId(UUID.randomUUID())
                .classId(schoolClass.getId())
                .academicYearId(year.getId())
                .isActive(true)
                .build();
        enrollment1.setCreatedAt(java.time.LocalDateTime.now());
        enrollment1.setUpdatedAt(java.time.LocalDateTime.now());
        enrollmentRepository.save(enrollment1);

        StudentClassEnrollment enrollment2 = StudentClassEnrollment.builder()
                .tenantId(tenantId)
                .studentId(UUID.randomUUID())
                .classId(schoolClass.getId())
                .academicYearId(year.getId())
                .isActive(true)
                .build();
        enrollment2.setCreatedAt(java.time.LocalDateTime.now());
        enrollment2.setUpdatedAt(java.time.LocalDateTime.now());
        enrollmentRepository.save(enrollment2);

        // Test
        List<TeacherAssignmentResponse> responses = teacherAssignmentRepository.findResponsesByStaffId(staff.getId(), tenantId);

        assertFalse(responses.isEmpty());
        TeacherAssignmentResponse response = responses.get(0);
        assertEquals(year.getId(), response.academicYearId());
        assertEquals("2026", response.academicYearName());
        assertEquals(2, response.enrolledCount());
        assertEquals("Grade 1 East", response.className());
        assertEquals("Mathematics", response.subjectName());
    }
}
