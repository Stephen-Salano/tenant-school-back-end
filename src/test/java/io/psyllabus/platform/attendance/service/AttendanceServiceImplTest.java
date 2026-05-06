package io.psyllabus.platform.attendance.service;

import io.psyllabus.platform.academic.entities.AcademicTerm;
import io.psyllabus.platform.academic.entities.SchoolClass;
import io.psyllabus.platform.academic.enums.AssignmentStatus;
import io.psyllabus.platform.academic.repository.AcademicTermRepository;
import io.psyllabus.platform.academic.repository.SchoolClassRepository;
import io.psyllabus.platform.academic.repository.StudentClassEnrollmentRepository;
import io.psyllabus.platform.academic.repository.TeacherAssignmentRepository;
import io.psyllabus.platform.attendance.dto.request.AttendanceBulkMarkRequest;
import io.psyllabus.platform.attendance.dto.request.AttendanceMarkRequest;
import io.psyllabus.platform.attendance.dto.responses.AttendanceMarkResultResponse;
import io.psyllabus.platform.attendance.entities.AttendanceRecord;
import io.psyllabus.platform.attendance.enums.AttendanceStatus;
import io.psyllabus.platform.attendance.repository.AttendanceRepository;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.auth.enums.AccessScope;
import io.psyllabus.platform.auth.enums.UserStatus;
import io.psyllabus.platform.auth.enums.UserType;
import io.psyllabus.platform.common.exception.DuplicateResourceException;
import io.psyllabus.platform.common.exception.InsufficientPermissionException;
import io.psyllabus.platform.common.exception.ValidationException;
import io.psyllabus.platform.common.security.ScopeGuard;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.guardian.repository.GuardianRepository;
import io.psyllabus.platform.guardian.repository.StudentGuardianRepository;
import io.psyllabus.platform.staff.entities.Staff;
import io.psyllabus.platform.staff.repository.StaffRepository;
import io.psyllabus.platform.student.entities.Student;
import io.psyllabus.platform.student.repository.StudentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplTest {

    @Mock private AttendanceRepository attendanceRepository;
    @Mock private SchoolClassRepository schoolClassRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private StaffRepository staffRepository;
    @Mock private GuardianRepository guardianRepository;
    @Mock private StudentGuardianRepository studentGuardianRepository;
    @Mock private StudentClassEnrollmentRepository enrollmentRepository;
    @Mock private TeacherAssignmentRepository teacherAssignmentRepository;
    @Mock private AcademicTermRepository academicTermRepository;
    @Mock private ScopeGuard scopeGuard;
    @Mock private AuditService auditService;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void markAttendance_succeedsForAssignedTeacher() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID termId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);

        User teacher = staffUser(userId);
        SchoolClass schoolClass = SchoolClass.builder().id(classId).tenantId(tenantId).name("Grade 4 Blue").build();
        Staff staff = Staff.builder().id(staffId).tenantId(tenantId).userId(userId).firstName("Jane").lastName("Teacher").build();
        Student student = Student.builder()
                .id(studentId)
                .tenantId(tenantId)
                .userId(UUID.randomUUID())
                .admissionNumber("S12345")
                .firstName("Ada")
                .lastName("Learner")
                .admissionDate(LocalDate.now())
                .build();
        AcademicTerm term = AcademicTerm.builder().id(termId).tenantId(tenantId).name("Term 2").build();
        AttendanceBulkMarkRequest request = new AttendanceBulkMarkRequest(
                classId,
                LocalDate.now(),
                "idem-1",
                List.of(new AttendanceMarkRequest(studentId, AttendanceStatus.PRESENT))
        );

        when(schoolClassRepository.findByIdAndTenantId(classId, tenantId)).thenReturn(Optional.of(schoolClass));
        when(staffRepository.findByUserIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(staff));
        when(teacherAssignmentRepository.existsByStaffIdAndClassIdAndTenantIdAndStatus(staffId, classId, tenantId, AssignmentStatus.ACTIVE))
                .thenReturn(true);
        when(academicTermRepository.findByTenantIdAndIsCurrentTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(tenantId, LocalDate.now(), LocalDate.now()))
                .thenReturn(Optional.of(term));
        when(attendanceRepository.findAllByTenantIdAndIdempotencyKey(tenantId, "idem-1")).thenReturn(List.of());
        when(attendanceRepository.existsByTenantIdAndClassIdAndDate(tenantId, classId, LocalDate.now())).thenReturn(false);
        when(enrollmentRepository.findActiveStudentIdsByClassIdAndTenantId(classId, tenantId)).thenReturn(List.of(studentId));
        when(studentRepository.findAllByIdInAndTenantId(List.of(studentId), tenantId)).thenReturn(List.of(student));

        AttendanceMarkResultResponse response = attendanceService.markAttendance(request, teacher);

        assertFalse(response.idempotent());
        assertEquals(1, response.markedCount());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AttendanceRecord>> recordsCaptor = ArgumentCaptor.forClass(List.class);
        verify(attendanceRepository).saveAll(recordsCaptor.capture());
        assertEquals(1, recordsCaptor.getValue().size());
        assertEquals(studentId, recordsCaptor.getValue().getFirst().getStudentId());
        verify(auditService).log(eq(io.psyllabus.platform.audit.enums.AuditAction.CREATE), eq("ATTENDANCE_RECORD"), eq(classId), contains("recordCount=1"));
    }

    @Test
    void markAttendance_returnsIdempotentSuccessWhenKeyAlreadyExists() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);

        User admin = adminUser(userId);
        SchoolClass schoolClass = SchoolClass.builder().id(classId).tenantId(tenantId).name("Grade 5").build();
        AttendanceRecord existing = AttendanceRecord.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .classId(classId)
                .studentId(UUID.randomUUID())
                .date(LocalDate.now())
                .status(AttendanceStatus.PRESENT)
                .markedBy(userId)
                .academicTermId(UUID.randomUUID())
                .idempotencyKey("idem-2")
                .build();

        when(schoolClassRepository.findByIdAndTenantId(classId, tenantId)).thenReturn(Optional.of(schoolClass));
        when(academicTermRepository.findByTenantIdAndIsCurrentTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(tenantId, LocalDate.now(), LocalDate.now()))
                .thenReturn(Optional.of(AcademicTerm.builder().id(UUID.randomUUID()).tenantId(tenantId).build()));
        when(attendanceRepository.findAllByTenantIdAndIdempotencyKey(tenantId, "idem-2")).thenReturn(List.of(existing));

        AttendanceMarkResultResponse response = attendanceService.markAttendance(
                new AttendanceBulkMarkRequest(
                        classId,
                        LocalDate.now(),
                        "idem-2",
                        List.of(new AttendanceMarkRequest(UUID.randomUUID(), AttendanceStatus.PRESENT))
                ),
                admin
        );

        assertTrue(response.idempotent());
        assertEquals(1, response.markedCount());
        verify(attendanceRepository, never()).saveAll(anyList());
    }

    @Test
    void markAttendance_rejectsDuplicateClassDay() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);

        when(schoolClassRepository.findByIdAndTenantId(classId, tenantId))
                .thenReturn(Optional.of(SchoolClass.builder().id(classId).tenantId(tenantId).name("Grade 6").build()));
        when(attendanceRepository.findAllByTenantIdAndIdempotencyKey(tenantId, "idem-3")).thenReturn(List.of());
        when(academicTermRepository.findByTenantIdAndIsCurrentTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(tenantId, LocalDate.now(), LocalDate.now()))
                .thenReturn(Optional.of(AcademicTerm.builder().id(UUID.randomUUID()).tenantId(tenantId).build()));
        when(attendanceRepository.existsByTenantIdAndClassIdAndDate(tenantId, classId, LocalDate.now())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> attendanceService.markAttendance(
                new AttendanceBulkMarkRequest(
                        classId,
                        LocalDate.now(),
                        "idem-3",
                        List.of(new AttendanceMarkRequest(UUID.randomUUID(), AttendanceStatus.PRESENT))
                ),
                adminUser(UUID.randomUUID())
        ));
    }

    @Test
    void markAttendance_rejectsTeacherWithoutAssignment() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);

        when(schoolClassRepository.findByIdAndTenantId(classId, tenantId))
                .thenReturn(Optional.of(SchoolClass.builder().id(classId).tenantId(tenantId).name("Grade 3").build()));
        when(staffRepository.findByUserIdAndTenantId(userId, tenantId))
                .thenReturn(Optional.of(Staff.builder().id(staffId).tenantId(tenantId).userId(userId).build()));
        when(teacherAssignmentRepository.existsByStaffIdAndClassIdAndTenantIdAndStatus(staffId, classId, tenantId, AssignmentStatus.ACTIVE))
                .thenReturn(false);

        assertThrows(InsufficientPermissionException.class, () -> attendanceService.markAttendance(
                new AttendanceBulkMarkRequest(
                        classId,
                        LocalDate.now(),
                        "idem-4",
                        List.of(new AttendanceMarkRequest(UUID.randomUUID(), AttendanceStatus.PRESENT))
                ),
                staffUser(userId)
        ));
    }

    @Test
    void markAttendance_rejectsStudentNotEnrolled() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID enrolledStudentId = UUID.randomUUID();
        UUID otherStudentId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);

        when(schoolClassRepository.findByIdAndTenantId(classId, tenantId))
                .thenReturn(Optional.of(SchoolClass.builder().id(classId).tenantId(tenantId).name("Grade 2").build()));
        when(staffRepository.findByUserIdAndTenantId(userId, tenantId))
                .thenReturn(Optional.of(Staff.builder().id(staffId).tenantId(tenantId).userId(userId).build()));
        when(teacherAssignmentRepository.existsByStaffIdAndClassIdAndTenantIdAndStatus(staffId, classId, tenantId, AssignmentStatus.ACTIVE))
                .thenReturn(true);
        when(academicTermRepository.findByTenantIdAndIsCurrentTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(tenantId, LocalDate.now(), LocalDate.now()))
                .thenReturn(Optional.of(AcademicTerm.builder().id(UUID.randomUUID()).tenantId(tenantId).build()));
        when(attendanceRepository.findAllByTenantIdAndIdempotencyKey(tenantId, "idem-5")).thenReturn(List.of());
        when(attendanceRepository.existsByTenantIdAndClassIdAndDate(tenantId, classId, LocalDate.now())).thenReturn(false);
        when(enrollmentRepository.findActiveStudentIdsByClassIdAndTenantId(classId, tenantId)).thenReturn(List.of(enrolledStudentId));

        assertThrows(ValidationException.class, () -> attendanceService.markAttendance(
                new AttendanceBulkMarkRequest(
                        classId,
                        LocalDate.now(),
                        "idem-5",
                        List.of(new AttendanceMarkRequest(otherStudentId, AttendanceStatus.ABSENT))
                ),
                staffUser(userId)
        ));
    }

    @Test
    void markAttendance_rejectsPastOrFutureDate() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);

        when(schoolClassRepository.findByIdAndTenantId(classId, tenantId))
                .thenReturn(Optional.of(SchoolClass.builder().id(classId).tenantId(tenantId).name("Grade 1").build()));

        assertThrows(ValidationException.class, () -> attendanceService.markAttendance(
                new AttendanceBulkMarkRequest(
                        classId,
                        LocalDate.now().minusDays(1),
                        "idem-6",
                        List.of(new AttendanceMarkRequest(UUID.randomUUID(), AttendanceStatus.PRESENT))
                ),
                adminUser(UUID.randomUUID())
        ));
    }

    @Test
    void markAttendance_rejectsWhenNoCurrentTermCoversToday() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);

        when(schoolClassRepository.findByIdAndTenantId(classId, tenantId))
                .thenReturn(Optional.of(SchoolClass.builder().id(classId).tenantId(tenantId).name("Grade 7").build()));
        when(academicTermRepository.findByTenantIdAndIsCurrentTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(tenantId, LocalDate.now(), LocalDate.now()))
                .thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> attendanceService.markAttendance(
                new AttendanceBulkMarkRequest(
                        classId,
                        LocalDate.now(),
                        "idem-7",
                        List.of(new AttendanceMarkRequest(UUID.randomUUID(), AttendanceStatus.PRESENT))
                ),
                adminUser(UUID.randomUUID())
        ));
    }

    private User adminUser(UUID userId) {
        return User.builder()
                .id(userId)
                .tenantId(TenantContext.getCurrentTenantId())
                .username("admin")
                .email("admin@example.com")
                .hashedPassword("secret")
                .firstName("Admin")
                .lastName("User")
                .userType(UserType.ADMIN)
                .accessScope(AccessScope.SCHOOL)
                .status(UserStatus.ACTIVE)
                .build();
    }

    private User staffUser(UUID userId) {
        return User.builder()
                .id(userId)
                .tenantId(TenantContext.getCurrentTenantId())
                .username("teacher")
                .email("teacher@example.com")
                .hashedPassword("secret")
                .firstName("Jane")
                .lastName("Teacher")
                .userType(UserType.STAFF)
                .accessScope(AccessScope.SCHOOL)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
