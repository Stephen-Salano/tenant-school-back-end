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
import io.psyllabus.platform.attendance.dto.request.AttendanceSearchFilterRequest;
import io.psyllabus.platform.attendance.dto.responses.AttendanceChildrenResponse;
import io.psyllabus.platform.attendance.dto.responses.AttendanceClassStatusResponse;
import io.psyllabus.platform.attendance.dto.responses.AttendanceMarkResultResponse;
import io.psyllabus.platform.attendance.dto.responses.AttendanceReportResponse;
import io.psyllabus.platform.attendance.dto.responses.AttendanceResponse;
import io.psyllabus.platform.attendance.dto.responses.ClassAttendanceResponse;
import io.psyllabus.platform.attendance.dto.responses.StudentAttendanceResponse;
import io.psyllabus.platform.attendance.entities.AttendanceRecord;
import io.psyllabus.platform.attendance.enums.AttendanceStatus;
import io.psyllabus.platform.attendance.repository.AttendanceRepository;
import io.psyllabus.platform.attendance.repository.AttendanceSpecifications;
import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.auth.enums.UserType;
import io.psyllabus.platform.common.exception.DuplicateResourceException;
import io.psyllabus.platform.common.exception.InsufficientPermissionException;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.exception.ValidationException;
import io.psyllabus.platform.common.security.ScopeGuard;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.guardian.entities.Guardian;
import io.psyllabus.platform.guardian.entities.StudentGuardian;
import io.psyllabus.platform.guardian.repository.GuardianRepository;
import io.psyllabus.platform.guardian.repository.StudentGuardianRepository;
import io.psyllabus.platform.staff.entities.Staff;
import io.psyllabus.platform.staff.repository.StaffRepository;
import io.psyllabus.platform.student.entities.Student;
import io.psyllabus.platform.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private static final String ATTENDANCE_ENTITY = "ATTENDANCE_RECORD";
    private static final String ATTENDANCE_READ_ENTITY = "ATTENDANCE_READ";

    private final AttendanceRepository attendanceRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final GuardianRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final StudentClassEnrollmentRepository enrollmentRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final AcademicTermRepository academicTermRepository;
    private final ScopeGuard scopeGuard;
    private final AuditService auditService;

    @Override
    public AttendanceMarkResultResponse markAttendance(AttendanceBulkMarkRequest request, User currentUser) {
        UUID tenantId = currentTenant();
        SchoolClass schoolClass = getClassOrThrow(request.classId(), tenantId);
        enforceClassAccess(currentUser, schoolClass, true);

        LocalDate today = LocalDate.now();
        if (!today.equals(request.date())) {
            throw new ValidationException("Attendance can only be marked for today");
        }

        AcademicTerm currentTerm = academicTermRepository
                .findByTenantIdAndIsCurrentTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(tenantId, today, today)
                .orElseThrow(() -> new ValidationException("No current academic term covers today"));

        List<AttendanceRecord> idempotentRecords = attendanceRepository
                .findAllByTenantIdAndIdempotencyKey(tenantId, request.idempotencyKey());
        if (!idempotentRecords.isEmpty()) {
            SchoolClass existingClass = getClassOrThrow(idempotentRecords.getFirst().getClassId(), tenantId);
            auditService.log(
                    AuditAction.CREATE,
                    ATTENDANCE_ENTITY,
                    existingClass.getId(),
                    "Idempotent attendance mark replay accepted: classId=%s, date=%s, recordCount=%s"
                            .formatted(existingClass.getId(), idempotentRecords.getFirst().getDate(), idempotentRecords.size())
            );
            return new AttendanceMarkResultResponse(
                    existingClass.getId(),
                    existingClass.getName(),
                    idempotentRecords.getFirst().getDate(),
                    idempotentRecords.size(),
                    request.idempotencyKey(),
                    true
            );
        }

        if (attendanceRepository.existsByTenantIdAndClassIdAndDate(tenantId, request.classId(), request.date())) {
            throw new DuplicateResourceException("Attendance has already been marked for this class today");
        }

        Set<UUID> requestStudentIds = request.records().stream()
                .map(AttendanceMarkRequest::studentId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requestStudentIds.size() != request.records().size()) {
            throw new ValidationException("Attendance request contains duplicate student IDs");
        }

        List<UUID> enrolledStudentIds = enrollmentRepository.findActiveStudentIdsByClassIdAndTenantId(request.classId(), tenantId);
        if (enrolledStudentIds.isEmpty()) {
            throw new ValidationException("Cannot mark attendance for a class with no active enrollments");
        }

        Set<UUID> enrolledSet = new LinkedHashSet<>(enrolledStudentIds);
        Set<UUID> missingFromRequest = new LinkedHashSet<>(enrolledSet);
        missingFromRequest.removeAll(requestStudentIds);
        if (!missingFromRequest.isEmpty()) {
            throw new ValidationException("Attendance must include every actively enrolled student in the class");
        }

        Set<UUID> notEnrolled = new LinkedHashSet<>(requestStudentIds);
        notEnrolled.removeAll(enrolledSet);
        if (!notEnrolled.isEmpty()) {
            throw new ValidationException("Attendance request contains students not actively enrolled in the class");
        }

        List<Student> students = studentRepository.findAllByIdInAndTenantId(new ArrayList<>(requestStudentIds), tenantId);
        if (students.size() != requestStudentIds.size()) {
            throw new ValidationException("One or more students were not found in the current tenant");
        }

        List<AttendanceRecord> records = request.records().stream()
                .map(entry -> AttendanceRecord.builder()
                        .tenantId(tenantId)
                        .classId(request.classId())
                        .studentId(entry.studentId())
                        .date(request.date())
                        .status(entry.status())
                        .markedBy(currentUser.getId())
                        .academicTermId(currentTerm.getId())
                        .idempotencyKey(request.idempotencyKey())
                        .build())
                .toList();

        attendanceRepository.saveAll(records);
        log.info("Attendance marked [tenant={}, classId={}, date={}, count={}]", tenantId, request.classId(), request.date(), records.size());
        auditService.log(
                AuditAction.CREATE,
                ATTENDANCE_ENTITY,
                request.classId(),
                "Attendance marked: classId=%s, date=%s, recordCount=%s".formatted(request.classId(), request.date(), records.size())
        );

        return new AttendanceMarkResultResponse(
                schoolClass.getId(),
                schoolClass.getName(),
                request.date(),
                records.size(),
                request.idempotencyKey(),
                false
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ClassAttendanceResponse getClassAttendance(UUID classId, LocalDate date, User currentUser) {
        UUID tenantId = currentTenant();
        SchoolClass schoolClass = getClassOrThrow(classId, tenantId);
        enforceClassAccess(currentUser, schoolClass, true);

        LocalDate targetDate = requireDate(date, "Attendance date is required");
        List<AttendanceRecord> records = attendanceRepository
                .findAllByTenantIdAndClassIdAndDateOrderByCreatedAtAsc(tenantId, classId, targetDate);

        List<AttendanceResponse> responses = toAttendanceResponses(records, tenantId, schoolClass.getName());
        auditService.log(
                AuditAction.READ,
                ATTENDANCE_READ_ENTITY,
                classId,
                "Viewed class attendance: classId=%s, date=%s, recordCount=%s".formatted(classId, targetDate, responses.size())
        );

        return new ClassAttendanceResponse(
                classId,
                schoolClass.getName(),
                targetDate,
                !records.isEmpty(),
                buildSummary(records),
                responses
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StudentAttendanceResponse getStudentAttendance(UUID studentId, LocalDate startDate, LocalDate endDate, User currentUser) {
        UUID tenantId = currentTenant();
        validateDateRange(startDate, endDate);

        Student student = studentRepository.findByIdAndTenantId(studentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        enforceStudentAttendanceAccess(currentUser, student);

        List<AttendanceRecord> records = attendanceRepository.findAll(
                AttendanceSpecifications.withFilters(new AttendanceSearchFilterRequest(
                        tenantId,
                        null,
                        studentId,
                        null,
                        null,
                        null,
                        null,
                        startDate,
                        endDate,
                        null
                )),
                Sort.by(Sort.Order.asc("date"), Sort.Order.asc("createdAt"))
        );

        List<AttendanceResponse> responses = toAttendanceResponses(records, tenantId, null);
        auditService.log(
                AuditAction.READ,
                ATTENDANCE_READ_ENTITY,
                studentId,
                "Viewed student attendance: studentId=%s, startDate=%s, endDate=%s, recordCount=%s"
                        .formatted(studentId, startDate, endDate, responses.size())
        );

        return new StudentAttendanceResponse(
                student.getId(),
                student.getAdmissionNumber(),
                fullName(student.getFirstName(), student.getLastName()),
                startDate,
                endDate,
                buildSummary(records),
                responses
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceChildrenResponse getChildrenAttendance(LocalDate startDate, LocalDate endDate, User currentUser) {
        UUID tenantId = currentTenant();
        validateDateRange(startDate, endDate);

        if (currentUser.getUserType() != UserType.PARENT) {
            throw new InsufficientPermissionException("Only parents can access linked children attendance");
        }

        Guardian guardian = guardianRepository.findByUserIdAndTenantId(currentUser.getId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Guardian profile not found"));

        List<StudentGuardian> links = studentGuardianRepository.findByGuardianIdAndTenantId(guardian.getId(), tenantId);
        List<UUID> childIds = links.stream().map(StudentGuardian::getStudentId).distinct().toList();
        if (childIds.isEmpty()) {
            return new AttendanceChildrenResponse(guardian.getId(), startDate, endDate, 0, List.of());
        }

        Map<UUID, Student> studentMap = studentRepository.findAllByIdInAndTenantId(childIds, tenantId).stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));

        List<AttendanceRecord> records = attendanceRepository.findAll(
                AttendanceSpecifications.withFilters(new AttendanceSearchFilterRequest(
                        tenantId,
                        null,
                        null,
                        childIds,
                        null,
                        null,
                        null,
                        startDate,
                        endDate,
                        null
                )),
                Sort.by(Sort.Order.asc("date"), Sort.Order.asc("createdAt"))
        );

        Map<UUID, List<AttendanceRecord>> byStudent = records.stream()
                .collect(Collectors.groupingBy(AttendanceRecord::getStudentId, LinkedHashMap::new, Collectors.toList()));

        List<StudentAttendanceResponse> children = childIds.stream()
                .map(studentMap::get)
                .filter(Objects::nonNull)
                .map(student -> new StudentAttendanceResponse(
                        student.getId(),
                        student.getAdmissionNumber(),
                        fullName(student.getFirstName(), student.getLastName()),
                        startDate,
                        endDate,
                        buildSummary(byStudent.getOrDefault(student.getId(), List.of())),
                        toAttendanceResponses(byStudent.getOrDefault(student.getId(), List.of()), tenantId, null)
                ))
                .toList();

        auditService.log(
                AuditAction.READ,
                ATTENDANCE_READ_ENTITY,
                guardian.getId(),
                "Viewed linked children attendance: guardianId=%s, childCount=%s, startDate=%s, endDate=%s"
                        .formatted(guardian.getId(), children.size(), startDate, endDate)
        );

        return new AttendanceChildrenResponse(guardian.getId(), startDate, endDate, children.size(), children);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceClassStatusResponse getClassAttendanceStatus(UUID classId, LocalDate date, User currentUser) {
        UUID tenantId = currentTenant();
        SchoolClass schoolClass = getClassOrThrow(classId, tenantId);
        enforceClassAccess(currentUser, schoolClass, true);

        LocalDate targetDate = date != null ? date : LocalDate.now();
        long markedCount = attendanceRepository.countByTenantIdAndClassIdAndDate(tenantId, classId, targetDate);
        long totalStudents = enrollmentRepository.countByClassIdAndTenantIdAndIsActiveTrue(classId, tenantId);

        auditService.log(
                AuditAction.READ,
                ATTENDANCE_READ_ENTITY,
                classId,
                "Viewed attendance status: classId=%s, date=%s, markedCount=%s".formatted(classId, targetDate, markedCount)
        );

        return new AttendanceClassStatusResponse(
                classId,
                schoolClass.getName(),
                targetDate,
                markedCount > 0,
                markedCount,
                totalStudents
        );
    }

    private UUID currentTenant() {
        return TenantContext.getCurrentTenantId();
    }

    private LocalDate requireDate(LocalDate date, String message) {
        if (date == null) {
            throw new ValidationException(message);
        }
        return date;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ValidationException("startDate and endDate are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new ValidationException("endDate cannot be before startDate");
        }
    }

    private SchoolClass getClassOrThrow(UUID classId, UUID tenantId) {
        return schoolClassRepository.findByIdAndTenantId(classId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));
    }

    private void enforceClassAccess(User currentUser, SchoolClass schoolClass, boolean requireStaffAssignment) {
        if (schoolClass.getBranchId() != null) {
            scopeGuard.canAccessBranch(schoolClass.getBranchId());
        }

        if (currentUser.getUserType() == UserType.STAFF && requireStaffAssignment) {
            Staff staff = staffRepository.findByUserIdAndTenantId(currentUser.getId(), currentTenant())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff profile not found"));

            boolean assigned = teacherAssignmentRepository.existsByStaffIdAndClassIdAndTenantIdAndStatus(
                    staff.getId(),
                    schoolClass.getId(),
                    currentTenant(),
                    AssignmentStatus.ACTIVE
            );
            if (!assigned) {
                throw new InsufficientPermissionException("You are not assigned to this class");
            }
        }

        if (currentUser.getUserType() != UserType.ADMIN && currentUser.getUserType() != UserType.STAFF) {
            throw new InsufficientPermissionException("You do not have permission to access class attendance");
        }
    }

    private void enforceStudentAttendanceAccess(User currentUser, Student student) {
        UUID tenantId = currentTenant();

        if (currentUser.getUserType() == UserType.ADMIN) {
            if (student.getCurrentClassId() != null) {
                enforceBranchScopeOnly(getClassOrThrow(student.getCurrentClassId(), tenantId));
            }
            return;
        }

        if (currentUser.getUserType() == UserType.STAFF) {
            if (student.getCurrentClassId() == null) {
                throw new InsufficientPermissionException("Student is not currently assigned to a class");
            }
            enforceClassAccess(currentUser, getClassOrThrow(student.getCurrentClassId(), tenantId), true);
            return;
        }

        if (currentUser.getUserType() == UserType.PARENT) {
            Guardian guardian = guardianRepository.findByUserIdAndTenantId(currentUser.getId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Guardian profile not found"));
            if (!studentGuardianRepository.existsByStudentIdAndGuardianIdAndTenantId(student.getId(), guardian.getId(), tenantId)) {
                throw new InsufficientPermissionException("You are not linked to this student");
            }
            return;
        }

        if (currentUser.getUserType() == UserType.STUDENT) {
            Student currentStudent = studentRepository.findByUserIdAndTenantId(currentUser.getId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
            if (!currentStudent.getId().equals(student.getId())) {
                throw new InsufficientPermissionException("Students can only view their own attendance");
            }
            return;
        }

        throw new InsufficientPermissionException("You do not have permission to access student attendance");
    }

    private void enforceBranchScopeOnly(SchoolClass schoolClass) {
        if (schoolClass.getBranchId() != null) {
            scopeGuard.canAccessBranch(schoolClass.getBranchId());
        }
    }

    private List<AttendanceResponse> toAttendanceResponses(List<AttendanceRecord> records, UUID tenantId, String defaultClassName) {
        if (records.isEmpty()) {
            return List.of();
        }

        Map<UUID, Student> studentsById = studentRepository.findAllByIdInAndTenantId(
                        records.stream().map(AttendanceRecord::getStudentId).distinct().toList(),
                        tenantId
                ).stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));

        Map<UUID, String> classNames = defaultClassName != null
                ? records.stream().map(AttendanceRecord::getClassId).distinct()
                .collect(Collectors.toMap(Function.identity(), ignored -> defaultClassName, (left, right) -> left))
                : schoolClassRepository.findAllByIdInAndTenantId(
                                records.stream().map(AttendanceRecord::getClassId).distinct().toList(),
                                tenantId
                        ).stream()
                        .collect(Collectors.toMap(SchoolClass::getId, SchoolClass::getName, (left, right) -> left));

        return records.stream()
                .map(attendanceRecord -> {
                    Student student = studentsById.get(attendanceRecord.getStudentId());
                    return new AttendanceResponse(
                            attendanceRecord.getId(),
                            attendanceRecord.getStudentId(),
                            student != null ? fullName(student.getFirstName(), student.getLastName()) : null,
                            student != null ? student.getAdmissionNumber() : null,
                            attendanceRecord.getClassId(),
                            classNames.get(attendanceRecord.getClassId()),
                            attendanceRecord.getAcademicTermId(),
                            attendanceRecord.getDate(),
                            attendanceRecord.getStatus(),
                            attendanceRecord.getMarkedBy(),
                            attendanceRecord.getCreatedAt()
                    );
                })
                .toList();
    }

    private AttendanceReportResponse buildSummary(List<AttendanceRecord> records) {
        long total = records.size();
        long present = records.stream().filter(attendanceRecord -> attendanceRecord.getStatus() == AttendanceStatus.PRESENT).count();
        long late = records.stream().filter(attendanceRecord -> attendanceRecord.getStatus() == AttendanceStatus.LATE).count();
        long absent = records.stream().filter(attendanceRecord -> attendanceRecord.getStatus() == AttendanceStatus.ABSENT).count();
        long excused = records.stream().filter(attendanceRecord -> attendanceRecord.getStatus() == AttendanceStatus.EXCUSED).count();
        double rate = total == 0 ? 0.0 : ((double) (present + late + excused) / total) * 100.0;

        return new AttendanceReportResponse(total, present, late, absent, excused, Math.round(rate * 100.0) / 100.0);
    }

    private String fullName(String firstName, String lastName) {
        return (firstName + " " + lastName).trim();
    }
}
