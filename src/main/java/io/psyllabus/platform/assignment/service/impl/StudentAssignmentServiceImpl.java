package io.psyllabus.platform.assignment.service.impl;

import io.psyllabus.platform.assignment.dto.request.AssignmentSubmitRequest;
import io.psyllabus.platform.assignment.dto.request.AttachmentMetadataRequest;
import io.psyllabus.platform.assignment.dto.response.AttachmentMetadataResponse;
import io.psyllabus.platform.assignment.dto.response.StudentAssignmentDetailResponse;
import io.psyllabus.platform.assignment.dto.response.StudentAssignmentResponse;
import io.psyllabus.platform.assignment.entities.Assignment;
import io.psyllabus.platform.assignment.entities.AssignmentAttachment;
import io.psyllabus.platform.assignment.entities.AssignmentSubmission;
import io.psyllabus.platform.assignment.entities.SubmissionAttachment;
import io.psyllabus.platform.assignment.enums.AssignmentStatus;
import io.psyllabus.platform.assignment.enums.SubmissionStatus;
import io.psyllabus.platform.assignment.enums.SubmissionType;
import io.psyllabus.platform.assignment.event.SubmissionReceivedEvent;
import io.psyllabus.platform.assignment.repository.AssignmentAttachmentRepository;
import io.psyllabus.platform.assignment.repository.AssignmentRepository;
import io.psyllabus.platform.assignment.repository.AssignmentSubmissionRepository;
import io.psyllabus.platform.assignment.repository.SubmissionAttachmentRepository;
import io.psyllabus.platform.assignment.service.StudentAssignmentService;
import io.psyllabus.platform.academic.entities.SchoolClass;
import io.psyllabus.platform.academic.entities.Subject;
import io.psyllabus.platform.academic.repository.SchoolClassRepository;
import io.psyllabus.platform.academic.repository.SubjectRepository;
import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.common.config.RabbitMQConstants;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.exception.ValidationException;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.grading.entities.GradingScheme;
import io.psyllabus.platform.grading.repository.GradingSchemeRepository;
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
public class StudentAssignmentServiceImpl implements StudentAssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final AssignmentAttachmentRepository assignmentAttachmentRepository;
    private final SubmissionAttachmentRepository submissionAttachmentRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final SubjectRepository subjectRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final GradingSchemeRepository gradingSchemeRepository;
    private final RabbitTemplate rabbitTemplate;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StudentAssignmentResponse> listAssignments(UUID currentUserId, SubmissionStatus status, Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Student student = findStudentByUser(currentUserId, tenantId);

        Page<AssignmentSubmission> page = status == null
                ? assignmentSubmissionRepository.findAllByStudentIdAndTenantId(student.getId(), tenantId, pageable)
                : assignmentSubmissionRepository.findAllByStudentIdAndTenantIdAndSubmissionStatus(student.getId(), tenantId, status, pageable);

        Map<UUID, Assignment> assignments = assignmentRepository.findAllByIdInAndTenantId(
                page.getContent().stream().map(AssignmentSubmission::getAssignmentId).distinct().toList(),
                tenantId
        ).stream().collect(Collectors.toMap(Assignment::getId, Function.identity()));
        Map<UUID, Subject> subjects = subjectRepository.findAllByIdInAndTenantId(
                assignments.values().stream().map(Assignment::getSubjectId).distinct().toList(),
                tenantId
        ).stream().collect(Collectors.toMap(Subject::getId, Function.identity()));
        Map<UUID, Staff> staff = staffRepository.findAllByIdInAndTenantId(
                assignments.values().stream().map(Assignment::getStaffId).distinct().toList(),
                tenantId
        ).stream().collect(Collectors.toMap(Staff::getId, Function.identity()));

        return PageResponse.from(page.map(submission -> toStudentAssignmentResponse(
                submission,
                assignments.get(submission.getAssignmentId()),
                subjects,
                staff,
                tenantId
        )));
    }

    @Override
    @Transactional(readOnly = true)
    public StudentAssignmentDetailResponse getAssignment(UUID assignmentId, UUID currentUserId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Student student = findStudentByUser(currentUserId, tenantId);
        AssignmentSubmission submission = assignmentSubmissionRepository.findByAssignmentIdAndStudentIdAndTenantId(assignmentId, student.getId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found for current student"));
        Assignment assignment = assignmentRepository.findByIdAndTenantId(assignmentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));
        return toStudentAssignmentDetailResponse(submission, assignment, tenantId);
    }

    @Override
    public StudentAssignmentDetailResponse submitAssignment(UUID assignmentId, AssignmentSubmitRequest request, UUID currentUserId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Student student = findStudentByUser(currentUserId, tenantId);
        AssignmentSubmission submission = assignmentSubmissionRepository.findByAssignmentIdAndStudentIdAndTenantId(assignmentId, student.getId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found for current student"));
        Assignment assignment = assignmentRepository.findByIdAndTenantId(assignmentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        if (submission.getSubmissionStatus() != SubmissionStatus.PENDING) {
            throw new ValidationException("Assignment has already been submitted");
        }
        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new ValidationException("Assignment is not open for submissions");
        }
        if (!StringUtils.hasText(request.textContent()) && (request.attachments() == null || request.attachments().isEmpty())) {
            throw new ValidationException("Submission must contain text content, attachments, or both");
        }

        validateAttachments(request.attachments());
        submissionAttachmentRepository.deleteAllBySubmissionIdAndTenantId(submission.getId(), tenantId);
        if (request.attachments() != null && !request.attachments().isEmpty()) {
            submissionAttachmentRepository.saveAll(request.attachments().stream()
                    .map(attachment -> SubmissionAttachment.builder()
                            .id(attachment.id())
                            .submissionId(submission.getId())
                            .tenantId(tenantId)
                            .url(attachment.url())
                            .path(attachment.path())
                            .mimeType(attachment.mimeType())
                            .size(attachment.size())
                            .originalFilename(attachment.originalFilename())
                            .build())
                    .toList());
        }

        submission.setTextContent(StringUtils.hasText(request.textContent()) ? request.textContent().trim() : null);
        submission.setSubmissionType(resolveSubmissionType(request));
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setLate(submission.getSubmittedAt().isAfter(assignment.getDueDate()));
        submission.setSubmissionStatus(SubmissionStatus.SUBMITTED);
        assignmentSubmissionRepository.save(submission);

        rabbitTemplate.convertAndSend(
                RabbitMQConstants.ASSIGNMENT_EXCHANGE,
                RabbitMQConstants.SUBMISSION_RECEIVED_KEY,
                new SubmissionReceivedEvent(
                        "submission:" + submission.getId(),
                        submission.getId(),
                        assignmentId,
                        student.getId(),
                        assignment.getStaffId(),
                        tenantId,
                        submission.getSubmissionType()
                )
        );

        auditService.log(AuditAction.UPDATE, "ASSIGNMENT_SUBMISSION", submission.getId(), "Student submitted assignment");
        return toStudentAssignmentDetailResponse(submission, assignment, tenantId);
    }

    private StudentAssignmentResponse toStudentAssignmentResponse(
            AssignmentSubmission submission,
            Assignment assignment,
            Map<UUID, Subject> subjects,
            Map<UUID, Staff> staff,
            UUID tenantId
    ) {
        Subject subject = assignment != null ? subjects.get(assignment.getSubjectId()) : null;
        Staff teacher = assignment != null ? staff.get(assignment.getStaffId()) : null;
        LocalDateTime dueDate = assignment != null ? assignment.getDueDate() : null;

        return new StudentAssignmentResponse(
                submission.getId(),
                submission.getAssignmentId(),
                assignment != null ? assignment.getTitle() : null,
                subject != null ? subject.getName() : null,
                assignment != null ? assignment.getDescription() : null,
                dueDate,
                dueDate != null && dueDate.isBefore(LocalDateTime.now()) && submission.getSubmissionStatus() == SubmissionStatus.PENDING,
                submission.getSubmittedAt(),
                submission.getSubmissionStatus(),
                teacher != null ? teacher.getFirstName() + " " + teacher.getLastName() : null,
                assignment != null ? assignmentAttachmentRepository.countByAssignmentIdAndTenantId(assignment.getId(), tenantId) : 0,
                submission.getScore(),
                assignment != null ? assignment.getMaxScore() : null,
                submission.getGradeLabel(),
                submission.getFeedback()
        );
    }

    private StudentAssignmentDetailResponse toStudentAssignmentDetailResponse(AssignmentSubmission submission, Assignment assignment, UUID tenantId) {
        Subject subject = subjectRepository.findByIdAndTenantId(assignment.getSubjectId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + assignment.getSubjectId()));
        SchoolClass schoolClass = schoolClassRepository.findByIdAndTenantId(assignment.getClassId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + assignment.getClassId()));
        Staff teacher = staffRepository.findByIdAndTenantId(assignment.getStaffId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + assignment.getStaffId()));
        Optional<GradingScheme> gradingScheme = gradingSchemeRepository.findByTenantIdAndIsDefaultTrue(tenantId);

        return new StudentAssignmentDetailResponse(
                submission.getId(),
                assignment.getId(),
                assignment.getTitle(),
                subject.getName(),
                schoolClass.getName(),
                assignment.getAssignmentType(),
                assignment.getDueDate(),
                assignment.getPostedAt(),
                assignment.getDueDate().isBefore(LocalDateTime.now()) && submission.getSubmissionStatus() == SubmissionStatus.PENDING,
                assignment.getMaxScore(),
                gradingScheme.map(GradingScheme::getName).orElse(null),
                assignment.getDescription(),
                assignment.getInstructions(),
                teacher.getFirstName() + " " + teacher.getLastName(),
                assignmentAttachmentRepository.findAllByAssignmentIdAndTenantId(assignment.getId(), tenantId).stream()
                        .map(this::toAttachmentResponse)
                        .toList(),
                submission.getSubmissionStatus(),
                submission.getSubmittedAt(),
                submission.getSubmissionType(),
                submission.getTextContent(),
                submissionAttachmentRepository.findAllBySubmissionIdAndTenantId(submission.getId(), tenantId).stream()
                        .map(this::toAttachmentResponse)
                        .toList(),
                submission.getScore(),
                submission.getGradeLabel(),
                submission.getFeedback(),
                submission.getGradedAt(),
                submission.isLate()
        );
    }

    private SubmissionType resolveSubmissionType(AssignmentSubmitRequest request) {
        boolean hasText = StringUtils.hasText(request.textContent());
        boolean hasAttachments = request.attachments() != null && !request.attachments().isEmpty();
        if (hasText && hasAttachments) {
            return SubmissionType.BOTH;
        }
        if (hasAttachments) {
            return SubmissionType.ATTACHMENT;
        }
        return SubmissionType.TEXT;
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

    private Student findStudentByUser(UUID userId, UUID tenantId) {
        return studentRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for current user"));
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
}
