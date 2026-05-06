package io.psyllabus.platform.attendance.repository;

import io.psyllabus.platform.attendance.dto.request.AttendanceSearchFilterRequest;
import io.psyllabus.platform.attendance.entities.AttendanceRecord;
import io.psyllabus.platform.attendance.enums.AttendanceStatus;
import io.psyllabus.platform.student.entities.Student;
import io.psyllabus.platform.student.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AttendanceRepositoryTest {

    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private StudentRepository studentRepository;

    @Test
    void specifications_supportExactDateRangeStatusStudentNameAndTermFiltering() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID termOneId = UUID.randomUUID();
        UUID termTwoId = UUID.randomUUID();

        Student studentOne = studentRepository.save(student(tenantId, "S1001", "Ada", "Lovelace"));
        Student studentTwo = studentRepository.save(student(tenantId, "S1002", "Grace", "Hopper"));

        attendanceRepository.save(attendance(tenantId, classId, studentOne.getId(), termOneId, LocalDate.of(2026, 5, 5), AttendanceStatus.PRESENT));
        attendanceRepository.save(attendance(tenantId, classId, studentOne.getId(), termTwoId, LocalDate.of(2026, 5, 6), AttendanceStatus.LATE));
        attendanceRepository.save(attendance(tenantId, classId, studentTwo.getId(), termOneId, LocalDate.of(2026, 5, 7), AttendanceStatus.ABSENT));

        assertEquals(1, attendanceRepository.findAll(
                AttendanceSpecifications.withFilters(new AttendanceSearchFilterRequest(
                        tenantId, null, null, null, null, null, LocalDate.of(2026, 5, 6), null, null, null
                ))
        ).size());

        assertEquals(2, attendanceRepository.findAll(
                AttendanceSpecifications.withFilters(new AttendanceSearchFilterRequest(
                        tenantId, null, null, null, null, null, null, LocalDate.of(2026, 5, 5), LocalDate.of(2026, 5, 6), null
                ))
        ).size());

        assertEquals(1, attendanceRepository.findAll(
                AttendanceSpecifications.withFilters(new AttendanceSearchFilterRequest(
                        tenantId, null, null, null, null, null, null, null, null, AttendanceStatus.ABSENT
                ))
        ).size());

        assertEquals(2, attendanceRepository.findAll(
                AttendanceSpecifications.withFilters(new AttendanceSearchFilterRequest(
                        tenantId, "ada", null, null, null, null, null, null, null, null
                ))
        ).size());

        assertEquals(1, attendanceRepository.findAll(
                AttendanceSpecifications.withFilters(new AttendanceSearchFilterRequest(
                        tenantId, null, null, null, null, termTwoId, null, null, null, null
                ))
        ).size());
    }

    private Student student(UUID tenantId, String admissionNumber, String firstName, String lastName) {
        Student student = Student.builder()
                .tenantId(tenantId)
                .userId(UUID.randomUUID())
                .admissionNumber(admissionNumber)
                .firstName(firstName)
                .lastName(lastName)
                .admissionDate(LocalDate.of(2026, 1, 10))
                .build();
        student.setCreatedAt(LocalDateTime.now());
        return student;
    }

    private AttendanceRecord attendance(
            UUID tenantId,
            UUID classId,
            UUID studentId,
            UUID termId,
            LocalDate date,
            AttendanceStatus status
    ) {
        AttendanceRecord attendanceRecord = AttendanceRecord.builder()
                .tenantId(tenantId)
                .classId(classId)
                .studentId(studentId)
                .date(date)
                .status(status)
                .markedBy(UUID.randomUUID())
                .academicTermId(termId)
                .idempotencyKey("idem-" + UUID.randomUUID())
                .build();
        attendanceRecord.setCreatedAt(LocalDateTime.now());
        return attendanceRecord;
    }
}
