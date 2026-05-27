package io.psyllabus.platform.assignment.repository;

import io.psyllabus.platform.academic.entities.SchoolClass;
import io.psyllabus.platform.academic.entities.Subject;
import io.psyllabus.platform.academic.repository.SchoolClassRepository;
import io.psyllabus.platform.academic.repository.SubjectRepository;
import io.psyllabus.platform.assignment.dto.response.AssignmentResponse;
import io.psyllabus.platform.assignment.entities.Assignment;
import io.psyllabus.platform.assignment.entities.AssignmentSubmission;
import io.psyllabus.platform.assignment.enums.AssignmentStatus;
import io.psyllabus.platform.assignment.enums.AssignmentType;
import io.psyllabus.platform.assignment.enums.SubmissionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AssignmentRepositoryTest {

    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private AssignmentSubmissionRepository submissionRepository;
    @Autowired private SchoolClassRepository classRepository;
    @Autowired private SubjectRepository subjectRepository;

    @Test
    void findResponsesWithStatsByClassId_ShouldReturnCorrectCounts() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();

        // Setup Class & Subject
        SchoolClass schoolClass = SchoolClass.builder()
                .tenantId(tenantId).name("Grade 1").academicYearId(UUID.randomUUID()).build();
        schoolClass.setCreatedAt(LocalDateTime.now());
        schoolClass.setUpdatedAt(LocalDateTime.now());
        schoolClass = classRepository.save(schoolClass);

        Subject subject = Subject.builder()
                .tenantId(tenantId).name("Math").code("M1").build();
        subject.setCreatedAt(LocalDateTime.now());
        subject.setUpdatedAt(LocalDateTime.now());
        subject = subjectRepository.save(subject);

        // Setup Assignment
        Assignment assignment = Assignment.builder()
                .tenantId(tenantId)
                .classId(schoolClass.getId())
                .subjectId(subject.getId())
                .staffId(UUID.randomUUID())
                .academicTermId(UUID.randomUUID())
                .title("Homework 1")
                .assignmentType(AssignmentType.HOMEWORK)
                .maxScore(100)
                .dueDate(LocalDateTime.now().plusDays(7))
                .status(AssignmentStatus.ACTIVE)
                .build();
        assignment.setCreatedAt(LocalDateTime.now());
        assignment.setUpdatedAt(LocalDateTime.now());
        assignment = assignmentRepository.save(assignment);

        // Setup Submissions
        saveSubmission(tenantId, assignment.getId(), SubmissionStatus.PENDING);
        saveSubmission(tenantId, assignment.getId(), SubmissionStatus.SUBMITTED);
        saveSubmission(tenantId, assignment.getId(), SubmissionStatus.SUBMITTED);
        saveSubmission(tenantId, assignment.getId(), SubmissionStatus.GRADED);

        // Test
        Page<AssignmentResponse> results = assignmentRepository.findResponsesWithStatsByClassId(schoolClass.getId(), tenantId, PageRequest.of(0, 10));

        assertEquals(1, results.getTotalElements());
        AssignmentResponse response = results.getContent().get(0);
        assertEquals(1L, response.pendingCount());
        assertEquals(2L, response.submittedCount());
        assertEquals(1L, response.gradedCount());
    }

    private void saveSubmission(UUID tenantId, UUID assignmentId, SubmissionStatus status) {
        AssignmentSubmission submission = AssignmentSubmission.builder()
                .tenantId(tenantId)
                .assignmentId(assignmentId)
                .studentId(UUID.randomUUID())
                .submissionStatus(status)
                .build();
        submission.setCreatedAt(LocalDateTime.now());
        submission.setUpdatedAt(LocalDateTime.now());
        submissionRepository.save(submission);
    }
}
