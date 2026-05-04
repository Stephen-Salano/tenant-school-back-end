package io.psyllabus.platform.assignment.service;

import io.psyllabus.platform.academic.entities.SchoolClass;
import io.psyllabus.platform.academic.entities.Subject;
import io.psyllabus.platform.academic.repository.SchoolClassRepository;
import io.psyllabus.platform.academic.repository.SubjectRepository;
import io.psyllabus.platform.assignment.dto.request.AssignmentSubmitRequest;
import io.psyllabus.platform.assignment.dto.response.StudentAssignmentDetailResponse;
import io.psyllabus.platform.assignment.entities.Assignment;
import io.psyllabus.platform.assignment.entities.AssignmentSubmission;
import io.psyllabus.platform.assignment.enums.AssignmentStatus;
import io.psyllabus.platform.assignment.enums.AssignmentType;
import io.psyllabus.platform.assignment.enums.SubmissionStatus;
import io.psyllabus.platform.assignment.repository.AssignmentAttachmentRepository;
import io.psyllabus.platform.assignment.repository.AssignmentRepository;
import io.psyllabus.platform.assignment.repository.AssignmentSubmissionRepository;
import io.psyllabus.platform.assignment.repository.SubmissionAttachmentRepository;
import io.psyllabus.platform.assignment.service.impl.StudentAssignmentServiceImpl;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.grading.repository.GradingSchemeRepository;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentAssignmentServiceImplTest {

    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AssignmentSubmissionRepository assignmentSubmissionRepository;
    @Mock private AssignmentAttachmentRepository assignmentAttachmentRepository;
    @Mock private SubmissionAttachmentRepository submissionAttachmentRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private StaffRepository staffRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private SchoolClassRepository schoolClassRepository;
    @Mock private GradingSchemeRepository gradingSchemeRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private AuditService auditService;

    @InjectMocks
    private StudentAssignmentServiceImpl studentAssignmentService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void submitAssignment_marksLateAndPublishesReceiptEvent() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);

        Student student = Student.builder().id(studentId).tenantId(tenantId).userId(userId).firstName("Ada").lastName("Student").build();
        Assignment assignment = Assignment.builder()
                .id(assignmentId)
                .tenantId(tenantId)
                .staffId(teacherId)
                .classId(classId)
                .subjectId(subjectId)
                .academicTermId(UUID.randomUUID())
                .title("Essay")
                .assignmentType(AssignmentType.PROJECT)
                .maxScore(50)
                .dueDate(LocalDateTime.now().minusHours(2))
                .status(AssignmentStatus.ACTIVE)
                .description("Write an essay")
                .instructions("Two pages")
                .build();
        AssignmentSubmission submission = AssignmentSubmission.builder()
                .id(submissionId)
                .assignmentId(assignmentId)
                .tenantId(tenantId)
                .studentId(studentId)
                .submissionStatus(SubmissionStatus.PENDING)
                .build();

        when(studentRepository.findByUserIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(student));
        when(assignmentSubmissionRepository.findByAssignmentIdAndStudentIdAndTenantId(assignmentId, studentId, tenantId))
                .thenReturn(Optional.of(submission));
        when(assignmentRepository.findByIdAndTenantId(assignmentId, tenantId)).thenReturn(Optional.of(assignment));
        when(subjectRepository.findByIdAndTenantId(subjectId, tenantId)).thenReturn(Optional.of(Subject.builder().id(subjectId).tenantId(tenantId).name("English").build()));
        when(schoolClassRepository.findByIdAndTenantId(classId, tenantId)).thenReturn(Optional.of(SchoolClass.builder().id(classId).tenantId(tenantId).name("Grade 5").build()));
        when(staffRepository.findByIdAndTenantId(teacherId, tenantId)).thenReturn(Optional.of(Staff.builder().id(teacherId).tenantId(tenantId).firstName("Jane").lastName("Teacher").build()));
        when(gradingSchemeRepository.findByTenantIdAndIsDefaultTrue(tenantId)).thenReturn(Optional.empty());
        when(assignmentAttachmentRepository.findAllByAssignmentIdAndTenantId(assignmentId, tenantId)).thenReturn(List.of());
        when(submissionAttachmentRepository.findAllBySubmissionIdAndTenantId(submissionId, tenantId)).thenReturn(List.of());

        StudentAssignmentDetailResponse response = studentAssignmentService.submitAssignment(
                assignmentId,
                new AssignmentSubmitRequest("My essay answer", List.of()),
                userId
        );

        assertEquals(SubmissionStatus.SUBMITTED, response.submissionStatus());
        assertTrue(response.late());
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), eventCaptor.capture());
        assertTrue(eventCaptor.getValue().toString().contains(submissionId.toString()));
    }
}
