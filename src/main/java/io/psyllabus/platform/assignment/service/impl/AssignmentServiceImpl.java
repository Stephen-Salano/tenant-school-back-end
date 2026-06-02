package io.psyllabus.platform.assignment.service.impl;

import io.psyllabus.platform.academic.entities.AcademicTerm;
import io.psyllabus.platform.academic.entities.SchoolClass;
import io.psyllabus.platform.academic.entities.Subject;
import io.psyllabus.platform.academic.repository.AcademicTermRepository;
import io.psyllabus.platform.academic.repository.SchoolClassRepository;
import io.psyllabus.platform.academic.repository.StudentClassEnrollmentRepository;
import io.psyllabus.platform.academic.repository.SubjectRepository;
import io.psyllabus.platform.academic.repository.TeacherAssignmentRepository;
import io.psyllabus.platform.assignment.dto.request.AssignmentCreateRequest;
import io.psyllabus.platform.assignment.dto.request.AssignmentUpdateRequest;
import io.psyllabus.platform.assignment.dto.request.AttachmentMetadataRequest;
import io.psyllabus.platform.assignment.dto.request.GradeSubmissionRequest;
import io.psyllabus.platform.assignment.dto.response.AssignmentDetailResponse;
import io.psyllabus.platform.assignment.dto.response.AssignmentResponse;
import io.psyllabus.platform.assignment.dto.response.AttachmentMetadataResponse;
import io.psyllabus.platform.assignment.dto.response.SubmissionResponse;
import io.psyllabus.platform.assignment.entities.Assignment;
import io.psyllabus.platform.assignment.entities.AssignmentAttachment;
import io.psyllabus.platform.assignment.entities.AssignmentSubmission;
import io.psyllabus.platform.assignment.entities.SubmissionAttachment;
import io.psyllabus.platform.assignment.enums.AssignmentStatus;
import io.psyllabus.platform.assignment.enums.SubmissionStatus;
import io.psyllabus.platform.assignment.event.AssignmentPublishedEvent;
import io.psyllabus.platform.assignment.event.SubmissionGradedEvent;
import io.psyllabus.platform.assignment.repository.AssignmentAttachmentRepository;
import io.psyllabus.platform.assignment.repository.AssignmentRepository;
import io.psyllabus.platform.assignment.repository.AssignmentSubmissionRepository;
import io.psyllabus.platform.assignment.repository.SubmissionAttachmentRepository;
import io.psyllabus.platform.assignment.service.AssignmentService;
import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.common.config.RabbitMQConstants;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.common.exception.InsufficientPermissionException;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.exception.ValidationException;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.grading.dto.response.GradeCalculationResponse;
import io.psyllabus.platform.grading.service.GradingSchemeService;
import io.psyllabus.platform.staff.entities.Staff;
import io.psyllabus.platform.staff.repository.StaffRepository;
import io.psyllabus.platform.student.entities.Student;
import io.psyllabus.platform.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AssignmentServiceImpl implements AssignmentService {

    private static final String ASSIGNMENT_ENTITY = "ASSIGNMENT";
    private static final String SUBMISSION_ENTITY = "ASSIGNMENT_SUBMISSION";
    public static final String ASSIGNMENT_NOT_FOUND = "Assignment not found: ";

    private final AssignmentRepository assignmentRepository;
    private final AssignmentAttachmentRepository assignmentAttachmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final SubmissionAttachmentRepository submissionAttachmentRepository;
    private final StaffRepository staffRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicTermRepository academicTermRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final StudentClassEnrollmentRepository studentClassEnrollmentRepository;
    private final GradingSchemeService gradingSchemeService;
    private final RabbitTemplate rabbitTemplate;
    private final AuditService auditService;

    @Override
    public AssignmentDetailResponse createAssignment(AssignmentCreateRequest request, UUID currentUserId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Staff currentStaff = findStaffByUser(currentUserId, tenantId);
        SchoolClass schoolClass = findClass(request.classId(), tenantId);
        Subject subject = findSubject(request.subjectId(), tenantId);
        AcademicTerm term = findCurrentTerm(tenantId);

        validateTeacherOwnership(currentStaff.getId(), schoolClass.getId(), subject.getId(), term.getId(), tenantId);

        Assignment assignment = assignmentRepository.save(Assignment.builder()
                .tenantId(tenantId)
                .staffId(currentStaff.getId())
                .classId(schoolClass.getId())
                .subjectId(subject.getId())
                .academicTermId(term.getId())
                .title(request.title().trim())
                .description(request.description())
                .instructions(request.instructions())
                .assignmentType(request.assignmentType())
                .maxScore(request.maxScore())
                .dueDate(request.dueDate())
                .status(AssignmentStatus.DRAFT)
                .build());

        replaceAssignmentAttachments(assignment.getId(), tenantId, request.attachments());
        auditService.log(AuditAction.CREATE, ASSIGNMENT_ENTITY, assignment.getId(), "Created assignment draft " + assignment.getTitle());
        return toAssignmentDetailResponse(assignment.getId(), tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AssignmentResponse> listAssignments(UUID currentUserId, UUID classId, AssignmentStatus status, Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Staff currentStaff = findStaffByUser(currentUserId, tenantId);

        // Optimization: If listing by class without specific status, use the enriched projection with stats
        if (classId != null && status == null) {
            return PageResponse.from(assignmentRepository.findResponsesWithStatsByClassId(classId, tenantId, pageable));
        }

        Page<Assignment> page = queryAssignments(currentStaff.getId(), classId, status, tenantId, pageable);
        Map<UUID, SchoolClass> classes = schoolClassRepository.findAllByIdInAndTenantId(
                page.getContent().stream().map(Assignment::getClassId).distinct().toList(),
                tenantId
        ).stream().collect(Collectors.toMap(SchoolClass::getId, Function.identity()));
        Map<UUID, Subject> subjects = subjectRepository.findAllByIdInAndTenantId(
                page.getContent().stream().map(Assignment::getSubjectId).distinct().toList(),
                tenantId
        ).stream().collect(Collectors.toMap(Subject::getId, Function.identity()));

        List<UUID> assignmentIds = page.getContent().stream().map(Assignment::getId).toList();

        Map<UUID, long[]> subCounts = assignmentSubmissionRepository
                .countSubmissionsByAssignmentIds(assignmentIds, tenantId)
                .stream().collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> new long[]{(long) row[1], (long) row[2], (long) row[3]}
                ));

        Map<UUID, Long> attachCounts = assignmentAttachmentRepository
                .countByAssignmentIds(assignmentIds, tenantId)
                .stream().collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));

        return PageResponse.from(page.map(assignment -> {
            long[] counts = subCounts.getOrDefault(assignment.getId(), new long[]{0, 0, 0});
            return toAssignmentResponse(
                    assignment,
                    classes.get(assignment.getClassId()),
                    subjects.get(assignment.getSubjectId()),
                    attachCounts.getOrDefault(assignment.getId(), 0L),
                    counts[0], counts[1], counts[2]
            );
        }));
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentDetailResponse getAssignment(UUID assignmentId, UUID currentUserId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Staff currentStaff = findStaffByUser(currentUserId, tenantId);
        resolveOwnedAssignment(assignmentId, currentStaff, tenantId);
        return toAssignmentDetailResponse(assignmentId, tenantId);
    }

    @Override
    public AssignmentDetailResponse updateAssignment(UUID assignmentId, AssignmentUpdateRequest request, UUID currentUserId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Staff currentStaff = findStaffByUser(currentUserId, tenantId);
        Assignment assignment = resolveOwnedAssignment(assignmentId, currentStaff, tenantId);

        if (assignment.getStatus() != AssignmentStatus.DRAFT) {
            throw new ValidationException("Only draft assignments can be updated");
        }

        if (StringUtils.hasText(request.title())) {
            assignment.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            assignment.setDescription(request.description());
        }
        if (request.instructions() != null) {
            assignment.setInstructions(request.instructions());
        }
        if (request.assignmentType() != null) {
            assignment.setAssignmentType(request.assignmentType());
        }
        if (request.maxScore() != null) {
            assignment.setMaxScore(request.maxScore());
        }
        if (request.dueDate() != null) {
            assignment.setDueDate(request.dueDate());
        }

        assignmentRepository.save(assignment);
        if (request.attachments() != null) {
            replaceAssignmentAttachments(assignmentId, tenantId, request.attachments());
        }

        auditService.log(AuditAction.UPDATE, ASSIGNMENT_ENTITY, assignment.getId(), "Updated assignment draft " + assignment.getTitle());
        return toAssignmentDetailResponse(assignmentId, tenantId);
    }

    @Override
    public AssignmentDetailResponse publishAssignment(UUID assignmentId, UUID currentUserId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Staff currentStaff = findStaffByUser(currentUserId, tenantId);
        Assignment assignment = resolveOwnedAssignment(assignmentId, currentStaff, tenantId);

        if (assignment.getStatus() != AssignmentStatus.DRAFT) {
            throw new ValidationException("Only draft assignments can be published");
        }
        if (!assignment.getDueDate().isAfter(LocalDateTime.now())) {
            throw new ValidationException("Due date must be in the future before publishing");
        }

        assignment.setStatus(AssignmentStatus.ACTIVE);
        assignment.setPostedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        rabbitTemplate.convertAndSend(
                RabbitMQConstants.ASSIGNMENT_EXCHANGE,
                RabbitMQConstants.ASSIGNMENT_PUBLISHED_KEY,
                new AssignmentPublishedEvent("published:" + assignment.getId(), assignment.getId(), tenantId)
        );

        auditService.log(AuditAction.UPDATE, ASSIGNMENT_ENTITY, assignment.getId(), "Published assignment " + assignment.getTitle());
        return toAssignmentDetailResponse(assignmentId, tenantId);
    }

    @Override
    public AssignmentDetailResponse closeAssignment(UUID assignmentId, UUID currentUserId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Staff currentStaff = findStaffByUser(currentUserId, tenantId);
        Assignment assignment = resolveOwnedAssignment(assignmentId, currentStaff, tenantId);

        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new ValidationException("Only active assignments can be closed");
        }

        assignment.setStatus(AssignmentStatus.CLOSED);
        assignment.setClosedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);
        auditService.log(AuditAction.UPDATE, ASSIGNMENT_ENTITY, assignment.getId(), "Closed assignment " + assignment.getTitle());
        return toAssignmentDetailResponse(assignmentId, tenantId);
    }

    @Override
    public void archiveAssignment(UUID assignmentId, UUID currentUserId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Staff currentStaff = findStaffByUser(currentUserId, tenantId);
        Assignment assignment = resolveOwnedAssignment(assignmentId, currentStaff, tenantId);

        if (assignment.getStatus() == AssignmentStatus.ARCHIVED) {
            throw new ValidationException("Assignment is already archived");
        }

        assignment.setStatus(AssignmentStatus.ARCHIVED);
        assignment.setArchivedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);
        auditService.log(AuditAction.DELETE, ASSIGNMENT_ENTITY, assignment.getId(), "Archived assignment " + assignment.getTitle());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubmissionResponse> listSubmissions(UUID assignmentId, SubmissionStatus status, UUID currentUserId, Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Staff currentStaff = findStaffByUser(currentUserId, tenantId);
        Assignment assignment = resolveOwnedAssignment(assignmentId, currentStaff, tenantId);

        Page<AssignmentSubmission> page = status == null
                ? assignmentSubmissionRepository.findAllByAssignmentIdAndTenantId(assignmentId, tenantId, pageable)
                : assignmentSubmissionRepository.findAllByAssignmentIdAndTenantIdAndSubmissionStatus(assignmentId, tenantId, status, pageable);

        List<UUID> submissionIds = page.getContent().stream().map(AssignmentSubmission::getId).toList();
        Map<UUID, Student> students = studentRepository.findAllByIdInAndTenantId(
                page.getContent().stream().map(AssignmentSubmission::getStudentId).distinct().toList(),
                tenantId
        ).stream().collect(Collectors.toMap(Student::getId, Function.identity()));

        Map<UUID, List<AttachmentMetadataResponse>> attachmentsBySubmission = submissionAttachmentRepository
                .findAllBySubmissionIdInAndTenantId(submissionIds, tenantId)
                .stream()
                .collect(Collectors.groupingBy(
                        SubmissionAttachment::getSubmissionId,
                        Collectors.mapping(this::toAttachmentResponse, Collectors.toList())
                ));

        return PageResponse.from(page.map(submission ->
                toSubmissionResponse(
                        submission,
                        students.get(submission.getStudentId()),
                        assignment.getMaxScore(),
                        attachmentsBySubmission.getOrDefault(submission.getId(), List.of()))));
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionResponse getSubmission(UUID submissionId, UUID currentUserId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Staff currentStaff = findStaffByUser(currentUserId, tenantId);
        AssignmentSubmission submission = assignmentSubmissionRepository.findByIdAndTenantId(submissionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found: " + submissionId));
        Assignment assignment = resolveOwnedAssignment(submission.getAssignmentId(), currentStaff, tenantId);
        Student student = studentRepository.findByIdAndTenantId(submission.getStudentId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + submission.getStudentId()));
        List<AttachmentMetadataResponse> attachments = submissionAttachmentRepository
                .findAllBySubmissionIdAndTenantId(submission.getId(), tenantId)
                .stream()
                .map(this::toAttachmentResponse)
                .toList();
        return toSubmissionResponse(submission, student, assignment.getMaxScore(), attachments);
    }

    @Override
    public SubmissionResponse gradeSubmission(UUID submissionId, GradeSubmissionRequest request, UUID currentUserId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Staff currentStaff = findStaffByUser(currentUserId, tenantId);
        AssignmentSubmission submission = assignmentSubmissionRepository.findByIdAndTenantId(submissionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found: " + submissionId));
        Assignment assignment = resolveOwnedAssignment(submission.getAssignmentId(), currentStaff, tenantId);

        if (submission.getSubmissionStatus() != SubmissionStatus.SUBMITTED) {
            throw new ValidationException("Only submitted assignments can be graded");
        }
        if (request.score() > assignment.getMaxScore()) {
            throw new ValidationException("Score cannot exceed max score of " + assignment.getMaxScore());
        }

        GradeCalculationResponse grade = gradingSchemeService.resolveGrade(tenantId, request.score(), assignment.getMaxScore());
        submission.setScore(request.score());
        submission.setGradeLabel(grade.label());
        submission.setFeedback(request.feedback());
        submission.setSubmissionStatus(SubmissionStatus.GRADED);
        submission.setGradedAt(LocalDateTime.now());
        submission.setGradedBy(currentStaff.getId());
        assignmentSubmissionRepository.save(submission);

        rabbitTemplate.convertAndSend(
                RabbitMQConstants.ASSIGNMENT_EXCHANGE,
                RabbitMQConstants.SUBMISSION_GRADED_KEY,
                new SubmissionGradedEvent(
                        "graded:" + submission.getId(),
                        submission.getId(),
                        assignment.getId(),
                        submission.getStudentId(),
                        tenantId,
                        submission.getScore(),
                        submission.getGradeLabel(),
                        submission.getFeedback()
                )
        );

        auditService.log(AuditAction.UPDATE, SUBMISSION_ENTITY, submission.getId(), "Graded assignment submission");
        Student student = studentRepository.findByIdAndTenantId(submission.getStudentId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + submission.getStudentId()));
        List<AttachmentMetadataResponse> attachments = submissionAttachmentRepository
                .findAllBySubmissionIdAndTenantId(submission.getId(), tenantId)
                .stream()
                .map(this::toAttachmentResponse)
                .toList();
        return toSubmissionResponse(submission, student, assignment.getMaxScore(), attachments);
    }

    @Override
    public void createPendingSubmissions(UUID assignmentId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Assignment assignment = assignmentRepository.findByIdAndTenantId(assignmentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(ASSIGNMENT_NOT_FOUND + assignmentId));

        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new ValidationException("Only active assignments can fan out submissions");
        }

        Set<UUID> existingStudentIds = new HashSet<>(assignmentSubmissionRepository.findStudentIdsByAssignmentIdAndTenantId(assignmentId, tenantId));
        List<AssignmentSubmission> submissions = studentClassEnrollmentRepository.findActiveStudentIdsByClassIdAndTenantId(assignment.getClassId(), tenantId)
                .stream()
                .filter(studentId -> !existingStudentIds.contains(studentId))
                .map(studentId -> AssignmentSubmission.builder()
                        .assignmentId(assignmentId)
                        .tenantId(tenantId)
                        .studentId(studentId)
                        .submissionStatus(SubmissionStatus.PENDING)
                        .isLate(false)
                        .build())
                .toList();

        if (!submissions.isEmpty()) {
            assignmentSubmissionRepository.saveAll(submissions);
        }

        log.info("Assignment fan-out completed [assignmentId={}, createdSubmissions={}]", assignmentId, submissions.size());
    }

    private Page<Assignment> queryAssignments(UUID staffId, UUID classId, AssignmentStatus status, UUID tenantId, Pageable pageable) {
        if (classId != null && status != null) {
            return assignmentRepository.findAllByStaffIdAndClassIdAndTenantIdAndStatus(staffId, classId, tenantId, status, pageable);
        }
        if (classId != null) {
            return assignmentRepository.findAllByStaffIdAndClassIdAndTenantId(staffId, classId, tenantId, pageable);
        }
        if (status != null) {
            return assignmentRepository.findAllByStaffIdAndTenantIdAndStatus(staffId, tenantId, status, pageable);
        }
        return assignmentRepository.findAllByStaffIdAndTenantId(staffId, tenantId, pageable);
    }

    private Assignment resolveOwnedAssignment(UUID assignmentId, Staff currentStaff, UUID tenantId) {
        Assignment assignment = assignmentRepository.findByIdAndTenantId(assignmentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(ASSIGNMENT_NOT_FOUND + assignmentId));
        if (!assignment.getStaffId().equals(currentStaff.getId())) {
            throw new InsufficientPermissionException("You do not own this assignment");
        }
        validateTeacherOwnership(currentStaff.getId(), assignment.getClassId(), assignment.getSubjectId(), assignment.getAcademicTermId(), tenantId);
        return assignment;
    }

    private void validateTeacherOwnership(UUID staffId, UUID classId, UUID subjectId, UUID academicTermId, UUID tenantId) {
        boolean ownsAssignment = teacherAssignmentRepository.existsByStaffIdAndClassIdAndSubjectIdAndAcademicTermIdAndTenantIdAndStatus(
                staffId,
                classId,
                subjectId,
                academicTermId,
                tenantId,
                io.psyllabus.platform.academic.enums.AssignmentStatus.ACTIVE
        );
        if (!ownsAssignment) {
            throw new InsufficientPermissionException("You are not assigned to teach this class and subject in the current term");
        }
    }

    private void replaceAssignmentAttachments(UUID assignmentId, UUID tenantId, List<AttachmentMetadataRequest> attachments) {
        validateAttachments(attachments);
        assignmentAttachmentRepository.deleteAllByAssignmentIdAndTenantId(assignmentId, tenantId);
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        assignmentAttachmentRepository.saveAll(attachments.stream()
                .map(attachment -> AssignmentAttachment.builder()
                        .id(attachment.id())
                        .assignmentId(assignmentId)
                        .tenantId(tenantId)
                        .url(attachment.url())
                        .path(attachment.path())
                        .mimeType(attachment.mimeType())
                        .size(attachment.size())
                        .originalFilename(attachment.originalFilename())
                        .build())
                .toList());
    }

    private AssignmentDetailResponse toAssignmentDetailResponse(UUID assignmentId, UUID tenantId) {
        AssignmentDetailResponse base = assignmentRepository.findDetailById(assignmentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(ASSIGNMENT_NOT_FOUND + assignmentId));

        List<AttachmentMetadataResponse> attachments = assignmentAttachmentRepository.findAllByAssignmentIdAndTenantId(assignmentId, tenantId).stream()
                .map(this::toAttachmentResponse)
                .toList();

        return new AssignmentDetailResponse(
                base.id(), base.classId(), base.className(), base.subjectId(), base.subjectName(),
                base.academicTermId(), base.staffId(), base.teacherName(), base.title(), base.description(),
                base.instructions(), base.assignmentType(), base.maxScore(), base.dueDate(), base.postedAt(),
                base.closedAt(), base.archivedAt(), base.status(), attachments
        );
    }

    private AssignmentResponse toAssignmentResponse(
            Assignment assignment, SchoolClass schoolClass, Subject subject,
            long attachmentCount, long pendingCount, long submittedCount, long gradedCount) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getClassId(),
                schoolClass != null ? schoolClass.getName() : null,
                assignment.getSubjectId(),
                subject != null ? subject.getName() : null,
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getAssignmentType(),
                assignment.getMaxScore(),
                assignment.getDueDate(),
                assignment.getPostedAt(),
                assignment.getStatus(),
                attachmentCount,
                pendingCount,
                submittedCount,
                gradedCount
        );
    }

    private SubmissionResponse toSubmissionResponse(
            AssignmentSubmission submission,
            Student student,
            Integer maxScore,
            List<AttachmentMetadataResponse> attachments
    ) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getAssignmentId(),
                submission.getStudentId(),
                student != null ? student.getFirstName() + " " + student.getLastName() : null,
                student != null ? student.getAdmissionNumber() : null,
                submission.getSubmissionStatus(),
                submission.getSubmissionType(),
                submission.getSubmittedAt(),
                submission.isLate(),
                submission.getTextContent(),
                attachments,
                submission.getScore(),
                maxScore,
                submission.getGradeLabel(),
                submission.getFeedback(),
                submission.getGradedAt()
        );
    }

    private AttachmentMetadataResponse toAttachmentResponse(AssignmentAttachment attachment) {
        return new AttachmentMetadataResponse(
                attachment.getId(),
                attachment.getUrl(),
                attachment.getPath(),
                attachment.getMimeType(),
                attachment.getSize(),
                attachment.getOriginalFilename()
        );
    }

    private AttachmentMetadataResponse toAttachmentResponse(SubmissionAttachment attachment) {
        return new AttachmentMetadataResponse(
                attachment.getId(),
                attachment.getUrl(),
                attachment.getPath(),
                attachment.getMimeType(),
                attachment.getSize(),
                attachment.getOriginalFilename()
        );
    }

    private void validateAttachments(List<AttachmentMetadataRequest> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }

        Set<String> ids = new HashSet<>();
        for (AttachmentMetadataRequest attachment : attachments) {
            if (!ids.add(attachment.id())) {
                throw new ValidationException("Duplicate attachment metadata id: " + attachment.id());
            }
            if (!StringUtils.hasText(attachment.url()) || !StringUtils.hasText(attachment.path())) {
                throw new ValidationException("Attachment URL and path are required");
            }
            if (attachment.size() == null || attachment.size() <= 0) {
                throw new ValidationException("Attachment size must be positive");
            }
        }
    }

    private Staff findStaffByUser(UUID userId, UUID tenantId) {
        return staffRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff profile not found for current user"));
    }

    private SchoolClass findClass(UUID classId, UUID tenantId) {
        return schoolClassRepository.findByIdAndTenantId(classId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));
    }

    private Subject findSubject(UUID subjectId, UUID tenantId) {
        return subjectRepository.findByIdAndTenantId(subjectId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));
    }

    private AcademicTerm findCurrentTerm(UUID tenantId) {
        return academicTermRepository.findByTenantIdAndIsCurrentTrue(tenantId)
                .orElseThrow(() -> new ValidationException("No current academic term is configured"));
    }
}
