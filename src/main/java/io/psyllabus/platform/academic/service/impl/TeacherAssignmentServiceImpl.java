package io.psyllabus.platform.academic.service.impl;

import io.psyllabus.platform.academic.dto.request.TeacherAssignmentCreateRequest;
import io.psyllabus.platform.academic.dto.response.TeacherAssignmentResponse;
import io.psyllabus.platform.academic.entities.AcademicTerm;
import io.psyllabus.platform.academic.entities.SchoolClass;
import io.psyllabus.platform.academic.entities.Subject;
import io.psyllabus.platform.academic.entities.TeacherAssignment;
import io.psyllabus.platform.academic.repository.AcademicTermRepository;
import io.psyllabus.platform.academic.repository.SchoolClassRepository;
import io.psyllabus.platform.academic.repository.SubjectRepository;
import io.psyllabus.platform.academic.repository.TeacherAssignmentRepository;
import io.psyllabus.platform.academic.service.TeacherAssignmentService;
import io.psyllabus.platform.common.exception.DuplicateResourceException;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.staff.entities.Staff;
import io.psyllabus.platform.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link TeacherAssignmentService}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TeacherAssignmentServiceImpl implements TeacherAssignmentService {

    public static final String UNKNOWN = "Unknown";
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final StaffRepository staffRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicTermRepository academicTermRepository;

    @Override
    public TeacherAssignmentResponse assignTeacher(TeacherAssignmentCreateRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        AssignmentValidationData data = validateAssignmentRequest(request, tenantId);

        if (request.isClassTeacher()) {
            handleClassTeacherDemotion(request.classId(), request.academicTermId(), tenantId);
        }

        TeacherAssignment assignment = TeacherAssignment.builder()
                .tenantId(tenantId)
                .staffId(request.staffId())
                .classId(request.classId())
                .subjectId(request.subjectId())
                .academicTermId(request.academicTermId())
                .isClassTeacher(request.isClassTeacher())
                .lessonsPerWeek(request.lessonsPerWeek())
                .status(request.status() != null ? request.status() : io.psyllabus.platform.academic.enums.AssignmentStatus.ACTIVE)
                .build();

        TeacherAssignment saved = teacherAssignmentRepository.save(assignment);
        return mapToResponse(saved, data.staff(), data.schoolClass(), data.subject(), data.term());
    }

    private AssignmentValidationData validateAssignmentRequest(TeacherAssignmentCreateRequest request, UUID tenantId) {
        Staff staff = staffRepository.findByIdAndTenantId(request.staffId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found: " + request.staffId()));

        SchoolClass schoolClass = schoolClassRepository.findByIdAndTenantId(request.classId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.classId()));

        Subject subject = subjectRepository.findByIdAndTenantId(request.subjectId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + request.subjectId()));

        AcademicTerm term = academicTermRepository.findByIdAndTenantId(request.academicTermId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic term not found: " + request.academicTermId()));

        if (!schoolClass.getAcademicYearId().equals(term.getAcademicYearId())) {
            throw new io.psyllabus.platform.common.exception.ValidationException("Class and term academic years do not match.");
        }

        if (teacherAssignmentRepository.existsByStaffIdAndClassIdAndSubjectIdAndAcademicTermIdAndTenantId(
                request.staffId(), request.classId(), request.subjectId(), request.academicTermId(), tenantId)) {
            throw new DuplicateResourceException("Teacher assignment already exists.");
        }

        return new AssignmentValidationData(staff, schoolClass, subject, term);
    }

    private void handleClassTeacherDemotion(UUID classId, UUID termId, UUID tenantId) {
        List<TeacherAssignment> existing = teacherAssignmentRepository
                .findAllByClassIdAndAcademicTermIdAndIsClassTeacherTrueAndTenantId(classId, termId, tenantId);

        if (!existing.isEmpty()) {
            existing.forEach(a -> a.setClassTeacher(false));
            teacherAssignmentRepository.saveAll(existing);
            log.info("Demoted {} existing class teacher(s) for class {} in term {}", existing.size(), classId, termId);
        }
    }

    /**
     * Internal DTO for validation results to avoid multiple DB lookups.
     */
    private record AssignmentValidationData(
            Staff staff,
            SchoolClass schoolClass,
            Subject subject,
            AcademicTerm term
    ) {}


    @Override
    @Transactional(readOnly = true)
    public List<TeacherAssignmentResponse> getAssignmentsByClass(UUID classId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return teacherAssignmentRepository.findAllByClassIdAndTenantId(classId, tenantId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherAssignmentResponse> getAssignmentsByStaff(UUID staffId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return teacherAssignmentRepository.findAllByStaffIdAndTenantId(staffId, tenantId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private TeacherAssignmentResponse mapToResponse(TeacherAssignment a) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        
        Staff staff = staffRepository.findByIdAndTenantId(a.getStaffId(), tenantId).orElse(null);
        SchoolClass sc = schoolClassRepository.findByIdAndTenantId(a.getClassId(), tenantId).orElse(null);
        Subject sub = subjectRepository.findByIdAndTenantId(a.getSubjectId(), tenantId).orElse(null);
        AcademicTerm term = academicTermRepository.findByIdAndTenantId(a.getAcademicTermId(), tenantId).orElse(null);

        return mapToResponse(a, staff, sc, sub, term);
    }

    private TeacherAssignmentResponse mapToResponse(TeacherAssignment a, Staff staff, SchoolClass sc, Subject sub, AcademicTerm term) {
        return new TeacherAssignmentResponse(
                a.getId(),
                a.getStaffId(),
                staff != null ? staff.getFirstName() + " " + staff.getLastName() : UNKNOWN,
                a.getClassId(),
                sc != null ? sc.getName() : UNKNOWN,
                a.getSubjectId(),
                sub != null ? sub.getName() : UNKNOWN,
                a.getAcademicTermId(),
                term != null ? term.getName() : UNKNOWN,
                a.isClassTeacher(),
                a.getLessonsPerWeek(),
                a.getStatus(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
