package io.psyllabus.platform.assignment.service;

import io.psyllabus.platform.academic.entities.AcademicTerm;
import io.psyllabus.platform.academic.entities.SchoolClass;
import io.psyllabus.platform.academic.entities.Subject;
import io.psyllabus.platform.academic.repository.AcademicTermRepository;
import io.psyllabus.platform.academic.repository.SchoolClassRepository;
import io.psyllabus.platform.academic.repository.StudentClassEnrollmentRepository;
import io.psyllabus.platform.academic.repository.SubjectRepository;
import io.psyllabus.platform.academic.repository.TeacherAssignmentRepository;
import io.psyllabus.platform.assignment.dto.request.AssignmentCreateRequest;
import io.psyllabus.platform.assignment.dto.request.AttachmentMetadataRequest;
import io.psyllabus.platform.assignment.dto.response.AssignmentDetailResponse;
import io.psyllabus.platform.assignment.entities.Assignment;
import io.psyllabus.platform.assignment.enums.AssignmentStatus;
import io.psyllabus.platform.assignment.enums.AssignmentType;
import io.psyllabus.platform.assignment.repository.AssignmentAttachmentRepository;
import io.psyllabus.platform.assignment.repository.AssignmentRepository;
import io.psyllabus.platform.assignment.repository.AssignmentSubmissionRepository;
import io.psyllabus.platform.assignment.repository.SubmissionAttachmentRepository;
import io.psyllabus.platform.assignment.service.impl.AssignmentServiceImpl;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.grading.service.GradingSchemeService;
import io.psyllabus.platform.staff.entities.Staff;
import io.psyllabus.platform.staff.repository.StaffRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceImplTest {

    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AssignmentAttachmentRepository assignmentAttachmentRepository;
    @Mock private AssignmentSubmissionRepository assignmentSubmissionRepository;
    @Mock private SubmissionAttachmentRepository submissionAttachmentRepository;
    @Mock private StaffRepository staffRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private SchoolClassRepository schoolClassRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private AcademicTermRepository academicTermRepository;
    @Mock private TeacherAssignmentRepository teacherAssignmentRepository;
    @Mock private StudentClassEnrollmentRepository studentClassEnrollmentRepository;
    @Mock private GradingSchemeService gradingSchemeService;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private AuditService auditService;

    @InjectMocks
    private AssignmentServiceImpl assignmentService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createAssignment_savesDraftWhenTeacherOwnsClassSubject() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID termId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);

        Staff staff = Staff.builder().id(staffId).tenantId(tenantId).userId(userId).firstName("Jane").lastName("Teacher").build();
        SchoolClass schoolClass = SchoolClass.builder().id(classId).tenantId(tenantId).name("Grade 5 Blue").build();
        Subject subject = Subject.builder().id(subjectId).tenantId(tenantId).name("Math").build();
        AcademicTerm term = AcademicTerm.builder().id(termId).tenantId(tenantId).name("Term 2").isCurrent(true).build();
        Assignment saved = Assignment.builder()
                .id(assignmentId)
                .tenantId(tenantId)
                .staffId(staffId)
                .classId(classId)
                .subjectId(subjectId)
                .academicTermId(termId)
                .title("Homework 1")
                .assignmentType(AssignmentType.HOMEWORK)
                .maxScore(100)
                .dueDate(LocalDateTime.now().plusDays(3))
                .status(AssignmentStatus.DRAFT)
                .build();

        when(staffRepository.findByUserIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(staff));
        when(schoolClassRepository.findByIdAndTenantId(classId, tenantId)).thenReturn(Optional.of(schoolClass));
        when(subjectRepository.findByIdAndTenantId(subjectId, tenantId)).thenReturn(Optional.of(subject));
        when(academicTermRepository.findByTenantIdAndIsCurrentTrue(tenantId)).thenReturn(Optional.of(term));
        when(teacherAssignmentRepository.existsByStaffIdAndClassIdAndSubjectIdAndAcademicTermIdAndTenantIdAndStatus(
                eq(staffId), eq(classId), eq(subjectId), eq(termId), eq(tenantId), any()))
                .thenReturn(true);
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(saved);
        when(assignmentAttachmentRepository.findAllByAssignmentIdAndTenantId(assignmentId, tenantId)).thenReturn(List.of());

        AssignmentDetailResponse response = assignmentService.createAssignment(
                new AssignmentCreateRequest(
                        classId,
                        subjectId,
                        "Homework 1",
                        "Chapter 3",
                        "Show all work",
                        AssignmentType.HOMEWORK,
                        100,
                        LocalDateTime.now().plusDays(3),
                        List.of(new AttachmentMetadataRequest("file-1", "https://file", "assignments/file-1", "application/pdf", 120L, "paper.pdf"))
                ),
                userId
        );

        assertEquals(AssignmentStatus.DRAFT, response.status());
        verify(assignmentAttachmentRepository).saveAll(anyList());
    }

    @Test
    void publishAssignment_setsActiveAndEmitsEvent() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID termId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);

        Staff staff = Staff.builder().id(staffId).tenantId(tenantId).userId(userId).firstName("Jane").lastName("Teacher").build();
        Assignment assignment = Assignment.builder()
                .id(assignmentId)
                .tenantId(tenantId)
                .staffId(staffId)
                .classId(classId)
                .subjectId(subjectId)
                .academicTermId(termId)
                .title("Homework 1")
                .assignmentType(AssignmentType.HOMEWORK)
                .maxScore(100)
                .dueDate(LocalDateTime.now().plusDays(1))
                .status(AssignmentStatus.DRAFT)
                .build();

        when(staffRepository.findByUserIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(staff));
        when(assignmentRepository.findByIdAndTenantId(assignmentId, tenantId)).thenReturn(Optional.of(assignment));
        when(teacherAssignmentRepository.existsByStaffIdAndClassIdAndSubjectIdAndAcademicTermIdAndTenantIdAndStatus(
                eq(staffId), eq(classId), eq(subjectId), eq(termId), eq(tenantId), any()))
                .thenReturn(true);
        when(staffRepository.findByIdAndTenantId(staffId, tenantId)).thenReturn(Optional.of(staff));
        when(schoolClassRepository.findByIdAndTenantId(classId, tenantId)).thenReturn(Optional.of(SchoolClass.builder().id(classId).tenantId(tenantId).name("Grade 5").build()));
        when(subjectRepository.findByIdAndTenantId(subjectId, tenantId)).thenReturn(Optional.of(Subject.builder().id(subjectId).tenantId(tenantId).name("Math").build()));
        when(academicTermRepository.findByIdAndTenantId(termId, tenantId)).thenReturn(Optional.of(AcademicTerm.builder().id(termId).tenantId(tenantId).name("Term 2").build()));
        when(assignmentAttachmentRepository.findAllByAssignmentIdAndTenantId(assignmentId, tenantId)).thenReturn(List.of());

        AssignmentDetailResponse response = assignmentService.publishAssignment(assignmentId, userId);

        assertEquals(AssignmentStatus.ACTIVE, response.status());
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), eventCaptor.capture());
        assertTrue(eventCaptor.getValue().toString().contains(assignmentId.toString()));
    }
}
