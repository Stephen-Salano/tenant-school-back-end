package io.psyllabus.platform.academic.service.impl;

import io.psyllabus.platform.academic.dto.request.ClassCreateRequest;
import io.psyllabus.platform.academic.dto.request.ClassUpdateRequest;
import io.psyllabus.platform.academic.dto.response.ClassDetailResponse;
import io.psyllabus.platform.academic.dto.response.ClassEnrollmentStatsResponse;
import io.psyllabus.platform.academic.dto.response.ClassResponse;
import io.psyllabus.platform.academic.entities.SchoolClass;
import io.psyllabus.platform.academic.entities.TeacherAssignment;
import io.psyllabus.platform.academic.enums.AcademicStatus;
import io.psyllabus.platform.academic.enums.AssignmentStatus;
import io.psyllabus.platform.academic.repository.AcademicYearRepository;
import io.psyllabus.platform.academic.repository.SchoolClassRepository;
import io.psyllabus.platform.academic.repository.TeacherAssignmentRepository;
import io.psyllabus.platform.academic.service.ClassService;
import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.branch.repository.BranchRepository;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.common.exception.DuplicateResourceException;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.exception.ValidationException;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.school.entities.SchoolProfile;
import io.psyllabus.platform.school.repository.SchoolProfileRepository;
import io.psyllabus.platform.staff.repository.StaffRepository;
import io.psyllabus.platform.student.dto.responses.StudentResponse;
import io.psyllabus.platform.student.enums.StudentStatus;
import io.psyllabus.platform.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of {@link ClassService}.
 *
 * <p><strong>Stream name validation flow:</strong></p>
 * <pre>
 * createClass(request)
 * → load SchoolProfile for tenant
 * → if streamNames list is non-null AND non-empty
 * → incoming streamName must exist in the list (case-insensitive)
 * → throws ValidationException otherwise
 * → if list is null/empty → validation skipped (school hasn't configured streams yet)
 * </pre>
 *
 * <p><strong>assignClassTeacher atomic demotion pattern:</strong></p>
 * <pre>
 * All TeacherAssignments for the class are loaded once.
 * Existing class teacher(s) flagged false — collected into a save list.
 * Target assignment (active, matching staffId) flagged true — added to the same list.
 * saveAll() fires in one round-trip.
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ClassServiceImpl implements ClassService {

    public static final String CLASS_NOT_FOUND = "Class not found: ";
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolProfileRepository schoolProfileRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final StudentRepository studentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final StaffRepository staffRepository;
    private final BranchRepository branchRepository;
    private final AuditService auditService;

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    public ClassResponse createClass(ClassCreateRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        // Verify academic year belongs to this tenant
        academicYearRepository.findByIdAndTenantId(request.academicYearId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Academic year not found: " + request.academicYearId()));

        validateStreamName(tenantId, request.streamName());
        requireBranchIfPresent(tenantId, request.branchId());

        // Prevent duplicate class name within the same academic year
        if (schoolClassRepository.existsByNameAndAcademicYearIdAndTenantId(
                request.name(), request.academicYearId(), tenantId)) {
            throw new DuplicateResourceException(
                    "Class '" + request.name() + "' already exists for this academic year");
        }

        SchoolClass schoolClass = SchoolClass.builder()
                .tenantId(tenantId)
                .name(request.name())
                .gradeLevel(request.gradeLevel())
                .streamName(request.streamName())
                .academicYearId(request.academicYearId())
                .branchId(request.branchId())
                .maxCapacity(request.maxCapacity())
                .status(AcademicStatus.ACTIVE)
                .build();

        SchoolClass saved = schoolClassRepository.save(schoolClass);
        log.info("Class created: id={} name={} stream={} tenant={}", saved.getId(), saved.getName(), saved.getStreamName(), tenantId);

        return mapToResponse(saved);
    }

    // ── Class Teacher Assignment ───────────────────────────────────────────────

    /**
     * Atomically demotes the previous class teacher and promotes the new one.
     *
     * <p>The target staff member must already have an ACTIVE {@link TeacherAssignment}
     * in this class (for any subject). If they have multiple active assignments in
     * this class (teaching different subjects), the first one found is promoted.
     * All are valid homeroom anchors — the class teacher flag is class-level, not subject-level.</p>
     */
    @Override
    public void assignClassTeacher(UUID classId, UUID staffId) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        // Verify class belongs to this tenant
        schoolClassRepository.findByIdAndTenantId(classId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(CLASS_NOT_FOUND + classId));

        List<TeacherAssignment> classAssignments = teacherAssignmentRepository.findAllByClassIdAndTenantId(classId, tenantId);

        List<TeacherAssignment> toSave = new ArrayList<>();

        // Step 1: Demote any existing class teacher(s)
        classAssignments.stream()
                .filter(TeacherAssignment::isClassTeacher)
                .forEach(a -> {
                    a.setClassTeacher(false);
                    toSave.add(a);
                    log.debug("Demoted class teacher: assignmentId={}", a.getId());
                });

        // Step 2: Promote the target staff member's active assignment
        TeacherAssignment target = classAssignments.stream()
                .filter(a -> a.getStaffId().equals(staffId) && a.getStatus() == AssignmentStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active teacher assignment found for staff " + staffId + " in class " + classId + ". Assign the teacher to a subject in this class first via POST /api/teacher-assignments."));

        target.setClassTeacher(true);
        toSave.add(target);

        teacherAssignmentRepository.saveAll(toSave);
        log.info("Class teacher assigned: staffId={} classId={} assignmentId={} tenant={}", staffId, classId, target.getId(), tenantId);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClassResponse> getClassesByYear(UUID yearId, Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return PageResponse.from(
                schoolClassRepository.findAllByAcademicYearIdAndTenantId(yearId, tenantId, pageable)
                        .map(this::mapToResponse)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ClassDetailResponse getClassById(UUID classId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        SchoolClass sc = schoolClassRepository.findByIdAndTenantId(classId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(CLASS_NOT_FOUND + classId));

        int enrolled = (int) studentRepository.countByCurrentClassIdAndTenantIdAndCurrentStatus(classId, tenantId, StudentStatus.ACTIVE);
        int available = sc.getMaxCapacity() != null ? Math.max(0, sc.getMaxCapacity() - enrolled) : 0;

        UUID teacherId = null;
        String teacherName = null;

        Optional<TeacherAssignment> classTeacherAssignment = teacherAssignmentRepository
                .findFirstByClassIdAndIsClassTeacherTrueAndTenantId(classId, tenantId);

        if (classTeacherAssignment.isPresent()) {
            teacherId = classTeacherAssignment.get().getStaffId();
            teacherName = staffRepository.findDisplayNameByIdAndTenantId(teacherId, tenantId).orElse(null);
        }

        String academicYearName = academicYearRepository.findNameByIdAndTenantId(sc.getAcademicYearId(), tenantId)
                .orElse(null);

        return mapToDetailResponse(sc, academicYearName, enrolled, available, teacherId, teacherName);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassEnrollmentStatsResponse getClassEnrollmentStats(UUID classId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        SchoolClass sc = schoolClassRepository.findByIdAndTenantId(classId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(CLASS_NOT_FOUND + classId));

        int enrolled = (int) studentRepository.countByCurrentClassIdAndTenantIdAndCurrentStatus(classId, tenantId, StudentStatus.ACTIVE);
        int available = sc.getMaxCapacity() != null ? Math.max(0, sc.getMaxCapacity() - enrolled) : 0;

        return new ClassEnrollmentStatsResponse(classId, sc.getName(), enrolled, sc.getMaxCapacity(), available);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getStreamNames() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return schoolProfileRepository.findBySchoolId(tenantId)
                .map(SchoolProfile::getStreamNames)
                .orElse(List.of());
    }

    // ── Update / Delete ───────────────────────────────────────────────────────

    @Override
    public ClassResponse updateClass(UUID classId, ClassUpdateRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        SchoolClass schoolClass = schoolClassRepository.findByIdAndTenantId(classId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(CLASS_NOT_FOUND + classId));

        // Validation logic
        validateUpdateRequest(classId, tenantId, schoolClass, request);
        // null safe field application
        applyUpdates(schoolClass, request);

        SchoolClass saved = schoolClassRepository.save(schoolClass);
        auditService.log(AuditAction.UPDATE, "CLASS", saved.getId(), "Class updated");
        return mapToResponse(saved);
    }

    private void validateUpdateRequest(UUID classId, UUID tenantId, SchoolClass schoolClass, ClassUpdateRequest request) {
        UUID targetAcademicYearId = request.academicYearId() != null
                ? request.academicYearId()
                : schoolClass.getAcademicYearId();
        String targetName = request.name() != null ? request.name() : schoolClass.getName();

        if (request.academicYearId() != null) {
            academicYearRepository.findByIdAndTenantId(request.academicYearId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Academic year not found: " + request.academicYearId()));
        }
        if (request.name() != null) requireText(request.name(), "Class name");
        if (request.gradeLevel() != null) requireText(request.gradeLevel(), "Grade level");
        if (request.streamName() != null) validateStreamName(tenantId, request.streamName());
        if (request.branchId() != null) requireBranchIfPresent(tenantId, request.branchId());
        if (request.maxCapacity() != null) {
            long enrolled = studentRepository.countByCurrentClassIdAndTenantIdAndCurrentStatus(
                    classId, tenantId, StudentStatus.ACTIVE);
            if (request.maxCapacity() < enrolled) {
                throw new ValidationException("Max capacity cannot be less than current active enrollment");
            }
        }
        if (schoolClassRepository.existsByNameAndAcademicYearIdAndTenantIdAndIdNot(
                targetName, targetAcademicYearId, tenantId, classId)) {
            throw new DuplicateResourceException(
                    "Class '" + targetName + "' already exists for this academic year");
        }
    }

    private void applyUpdates(SchoolClass schoolClass, ClassUpdateRequest request) {
        if (request.name() != null) schoolClass.setName(request.name());
        if (request.gradeLevel() != null) schoolClass.setGradeLevel(request.gradeLevel());
        if (request.streamName() != null) schoolClass.setStreamName(request.streamName());
        if (request.academicYearId() != null) schoolClass.setAcademicYearId(request.academicYearId());
        if (request.branchId() != null) schoolClass.setBranchId(request.branchId());
        if (request.maxCapacity() != null) schoolClass.setMaxCapacity(request.maxCapacity());
        if (request.isActive() != null) schoolClass.setActive(request.isActive());
    }

    @Override
    public void deleteClass(UUID classId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        SchoolClass schoolClass = schoolClassRepository.findByIdAndTenantId(classId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(CLASS_NOT_FOUND + classId));

        schoolClass.setActive(false);
        schoolClassRepository.save(schoolClass);
        auditService.log(AuditAction.DELETE, "CLASS", schoolClass.getId(), "Class soft deleted");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> getClassStudents(UUID classId, Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        schoolClassRepository.findByIdAndTenantId(classId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(CLASS_NOT_FOUND + classId));
        return PageResponse.from(
                studentRepository.findByCurrentClassIdAndTenantIdAndCurrentStatus(
                        classId, tenantId, StudentStatus.ACTIVE, pageable
                ).map(student -> new StudentResponse(
                        student.getId(),
                        student.getAdmissionNumber(),
                        student.getFirstName(),
                        student.getLastName(),
                        student.getCurrentStatus()
                ))
        );
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private ClassResponse mapToResponse(SchoolClass sc) {
        return new ClassResponse(
                sc.getId(),
                sc.getName(),
                sc.getGradeLevel(),
                sc.getStreamName(),
                sc.getAcademicYearId(),
                sc.getBranchId(),
                sc.getMaxCapacity(),
                sc.isActive(),
                sc.getCreatedAt(),
                sc.getUpdatedAt()
        );
    }

    private ClassDetailResponse mapToDetailResponse(SchoolClass sc, String academicYearName, int enrolled, int available, UUID teacherId, String teacherName) {
        return new ClassDetailResponse(
                sc.getId(),
                sc.getName(),
                sc.getGradeLevel(),
                sc.getStreamName(),
                sc.getAcademicYearId(),
                academicYearName,
                sc.getBranchId(),
                sc.getMaxCapacity(),
                sc.isActive(),
                enrolled,
                available,
                teacherId,
                teacherName,
                sc.getCreatedAt(),
                sc.getUpdatedAt()
        );
    }

    private void validateStreamName(UUID tenantId, String streamName) {
        if (streamName == null || streamName.isBlank()) {
            return;
        }

        schoolProfileRepository.findBySchoolId(tenantId).ifPresent(profile -> {
            List<String> configured = profile.getStreamNames();
            if (configured != null && !configured.isEmpty()) {
                boolean valid = configured.stream()
                        .anyMatch(s -> s.equalsIgnoreCase(streamName));
                if (!valid) {
                    throw new ValidationException(
                            "Stream name '" + streamName + "' is not configured for this school. " +
                            "Valid streams: " + String.join(", ", configured));
                }
            }
        });
    }

    private void requireBranchIfPresent(UUID tenantId, UUID branchId) {
        if (branchId != null && !branchRepository.existsByIdAndTenantId(branchId, tenantId)) {
            throw new ResourceNotFoundException("Branch not found: " + branchId);
        }
    }

    private void requireText(String value, String fieldName) {
        if (value.isBlank()) {
            throw new ValidationException(fieldName + " cannot be blank");
        }
    }
}
