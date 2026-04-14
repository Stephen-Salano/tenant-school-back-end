package io.psyllabus.platform.onboarding.service.impl;

import io.psyllabus.platform.academic.dto.request.TeacherAssignmentCreateRequest;
import io.psyllabus.platform.academic.entities.SchoolClass;
import io.psyllabus.platform.academic.enums.AssignmentStatus;
import io.psyllabus.platform.academic.repository.AcademicTermRepository;
import io.psyllabus.platform.academic.repository.SchoolClassRepository;
import io.psyllabus.platform.academic.repository.SubjectGroupRepository;
import io.psyllabus.platform.academic.repository.SubjectRepository;
import io.psyllabus.platform.academic.service.ClassService;
import io.psyllabus.platform.academic.service.TeacherAssignmentService;
import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.auth.enums.UserType;
import io.psyllabus.platform.branch.entities.Branch;
import io.psyllabus.platform.branch.repository.BranchRepository;
import io.psyllabus.platform.common.annotation.SystemProcess;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.service.IdentifierService;
import io.psyllabus.platform.guardian.dto.request.GuardianCreateRequest;
import io.psyllabus.platform.guardian.entities.Guardian;
import io.psyllabus.platform.guardian.enums.RelationshipType;
import io.psyllabus.platform.guardian.repository.GuardianRepository;
import io.psyllabus.platform.guardian.repository.StudentGuardianRepository;
import io.psyllabus.platform.guardian.services.GuardianService;
import io.psyllabus.platform.messaging.dto.EmailMessage;
import io.psyllabus.platform.messaging.enums.EmailTemplateType;
import io.psyllabus.platform.messaging.service.EmailService;
import io.psyllabus.platform.onboarding.dto.ImportErrorDetail;
import io.psyllabus.platform.onboarding.dto.ImportRowOutcome;
import io.psyllabus.platform.onboarding.entity.ImportJob;
import io.psyllabus.platform.onboarding.repository.ImportJobRepository;
import io.psyllabus.platform.onboarding.service.OnboardingBatchService;
import io.psyllabus.platform.school.entities.School;
import io.psyllabus.platform.school.repository.SchoolRepository;
import io.psyllabus.platform.staff.dto.request.StaffCreateRequest;
import io.psyllabus.platform.staff.dto.responses.StaffResponse;
import io.psyllabus.platform.staff.services.StaffService;
import io.psyllabus.platform.student.dto.request.StudentCreateRequest;
import io.psyllabus.platform.student.dto.responses.StudentResponse;
import io.psyllabus.platform.student.enums.Gender;
import io.psyllabus.platform.student.enums.StudentStatus;
import io.psyllabus.platform.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SystemProcess
public class OnboardingBatchServiceImpl implements OnboardingBatchService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String IMPORT_JOB_LITERAL = "IMPORT_JOB";

    private final ImportJobRepository importJobRepository;
    private final StaffService staffService;
    private final StudentService studentService;
    private final GuardianService guardianService;
    private final TeacherAssignmentService teacherAssignmentService;
    private final ClassService classService;
    private final GuardianRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final BranchRepository branchRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectGroupRepository subjectGroupRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicTermRepository academicTermRepository;
    private final IdentifierService identifierService;
    private final AuditService auditService;
    private final EmailService emailService;
    private final SchoolRepository schoolRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImportJob markProcessing(UUID jobId, UUID tenantId) {
        ImportJob job = findJob(jobId, tenantId);
        if (job.getStatus() == io.psyllabus.platform.onboarding.enums.ImportJobStatus.QUEUED) {
            job.markProcessing();
            importJobRepository.save(job);
            auditService.log(AuditAction.BULK_IMPORT, IMPORT_JOB_LITERAL, jobId, "Job processing started");
        }
        return job;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchResult processStaffBatch(UUID jobId, UUID tenantId, List<Map<String, String>> rows) {
        ImportJob job = findJob(jobId, tenantId);
        List<ImportErrorDetail> errors = new ArrayList<>();
        List<ImportRowOutcome> outcomes = new ArrayList<>();
        int successes = 0;

        for (int offset = 0; offset < rows.size(); offset++) {
            Map<String, String> row = rows.get(offset);
            int rowNumber = job.getProcessedRows() + offset + 1;
            try {
                String identifier = createStaffFromRow(row, tenantId);
                successes++;
                outcomes.add(new ImportRowOutcome(rowNumber, "SUCCESS", identifier, "Created successfully"));
            } catch (Exception ex) {
                ImportErrorDetail error = toRowError(rowNumber, ex);
                errors.add(error);
                outcomes.add(new ImportRowOutcome(rowNumber, "FAILED", normalize(row.get("employee_number")), error.issue()));
            }
        }

        job.recordBatchResult(successes, errors, outcomes);
        importJobRepository.save(job);
        auditService.log(
                AuditAction.BULK_IMPORT,
                IMPORT_JOB_LITERAL,
                jobId,
                "Staff batch processed: size=" + rows.size() + ", success=" + successes + ", errors=" + errors.size()
        );
        return new BatchResult(successes, errors, outcomes);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchResult processStudentBatch(UUID jobId, UUID tenantId, List<Map<String, String>> rows) {
        ImportJob job = findJob(jobId, tenantId);
        List<ImportErrorDetail> errors = new ArrayList<>();
        List<ImportRowOutcome> outcomes = new ArrayList<>();
        int successes = 0;

        for (int offset = 0; offset < rows.size(); offset++) {
            Map<String, String> row = rows.get(offset);
            int rowNumber = job.getProcessedRows() + offset + 1;
            try {
                String identifier = createStudentAndGuardiansFromRow(row, tenantId);
                successes++;
                outcomes.add(new ImportRowOutcome(rowNumber, "SUCCESS", identifier, "Enrolled successfully"));
            } catch (Exception ex) {
                ImportErrorDetail error = toRowError(rowNumber, ex);
                errors.add(error);
                outcomes.add(new ImportRowOutcome(rowNumber, "FAILED", normalize(row.get("admission_number")), error.issue()));
            }
        }

        job.recordBatchResult(successes, errors, outcomes);
        importJobRepository.save(job);
        auditService.log(
                AuditAction.BULK_IMPORT,
                IMPORT_JOB_LITERAL,
                jobId,
                "Student batch processed: size=" + rows.size() + ", success=" + successes + ", errors=" + errors.size()
        );
        return new BatchResult(successes, errors, outcomes);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImportJob finalizeJob(UUID jobId, UUID tenantId) {
        ImportJob job = findJob(jobId, tenantId);
        job.complete();
        importJobRepository.save(job);
        
        sendAdminNotification(job);
        
        auditService.log(
                AuditAction.BULK_IMPORT,
                IMPORT_JOB_LITERAL,
                jobId,
                "Job completed with successRows=" + job.getSuccessRows() + ", errorRows=" + job.getErrorRows()
        );
        return job;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImportJob failJob(UUID jobId, UUID tenantId, String reason) {
        ImportJob job = findJob(jobId, tenantId);
        job.fail(reason);
        importJobRepository.save(job);
        
        sendAdminNotification(job);
        
        auditService.log(AuditAction.BULK_IMPORT, IMPORT_JOB_LITERAL, jobId, "Job failed: " + reason);
        return job;
    }
    
    private void sendAdminNotification(ImportJob job) {
        try {
            School school = schoolRepository.findById(job.getTenantId())
                    .orElseThrow(() -> new ResourceNotFoundException("School not found: " + job.getTenantId()));
            
            if (school.getEmail() == null || school.getEmail().isBlank()) {
                log.warn("School admin email not configured for tenant {}, skipping notification", job.getTenantId());
                return;
            }

            EmailTemplateType templateType = job.getStatus() == io.psyllabus.platform.onboarding.enums.ImportJobStatus.FAILED
                    ? EmailTemplateType.IMPORT_FAILED
                    : EmailTemplateType.IMPORT_COMPLETE_SUMMARY;

            EmailMessage message = new EmailMessage(
                    UUID.randomUUID().toString(),
                    job.getTenantId(),
                    school.getEmail(),
                    school.getName() + " Admin",
                    UserType.ADMIN,
                    templateType,
                    Map.of(
                            "successCount", String.valueOf(job.getSuccessRows()),
                            "failureCount", String.valueOf(job.getErrorRows()),
                            "totalRows", String.valueOf(job.getTotalRows()),
                            "jobType", job.getJobType().name(),
                            "jobId", job.getId().toString(),
                            "toName", school.getName() + " Admin"
                    )
            );
            
            emailService.send(message);
            log.info("Admin notification triggered for import job {} [status={}]", job.getId(), job.getStatus());
        } catch (Exception ex) {
            log.error("Failed to send admin notification for job {}", job.getId(), ex);
        }
    }

    private String createStaffFromRow(Map<String, String> row, UUID tenantId) {
        String employeeNumber = normalize(row.get("employee_number"));
        if (employeeNumber.isBlank()) {
            employeeNumber = identifierService.generateStaffId(tenantId);
        }

        UUID branchId = resolveBranchId(row.get("branch_code"), tenantId);
        UUID subjectGroupId = resolveSubjectGroupId(row.get("subject_group_name"), tenantId);

        StaffResponse created = staffService.createStaff(new StaffCreateRequest(
                employeeNumber,
                required(row, "first_name"),
                required(row, "last_name"),
                required(row, "email").toLowerCase(Locale.ROOT),
                normalize(row.get("phone")),
                subjectGroupId,
                parseDate(required(row, "hire_date")),
                normalize(row.get("qualification")),
                normalize(row.get("specialization")),
                branchId
        ));

        if (subjectGroupId != null) {
            staffService.assignSubjectGroup(created.id(), subjectGroupId);
        }

        createImportedTeachingAssignments(created.id(), subjectGroupId, row, tenantId);
        return employeeNumber;
    }

    private void createImportedTeachingAssignments(UUID staffId, UUID subjectGroupId, Map<String, String> row, UUID tenantId) {
        UUID currentTermId = academicTermRepository.findByTenantIdAndIsCurrentTrue(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No current academic term configured for this school."))
                .getId();

        String teachingAssignments = normalize(row.get("teaching_assignments"));
        String classTeacherAssignment = normalize(row.get("class_teacher_assignment"));

        if (teachingAssignments.isBlank()) {
            if (!classTeacherAssignment.isBlank()) {
                throw new ResourceNotFoundException("class_teacher_assignment requires at least one explicit teaching assignment.");
            }
            return;
        }

        List<ImportedTeachingAssignment> parsedAssignments = splitAssignments(teachingAssignments).stream()
                .map(this::parseAssignment)
                .toList();
        ImportedTeachingAssignment classTeacher = classTeacherAssignment.isBlank() ? null : parseAssignment(classTeacherAssignment);

        boolean classTeacherMatched = classTeacher == null;

        for (ImportedTeachingAssignment assignment : parsedAssignments) {
            SchoolClass schoolClass = resolveClass(assignment.gradeLevel(), assignment.streamName(), tenantId);
            var subject = subjectRepository.findByNameAndTenantId(assignment.subject(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + assignment.subject()));

            if (subjectGroupId != null && subject.getSubjectGroupId() != null && !subject.getSubjectGroupId().equals(subjectGroupId)) {
                throw new ResourceNotFoundException(
                        "Imported subject '" + assignment.subject() + "' does not belong to the imported subject_group_name."
                );
            }

            boolean isClassTeacher = classTeacher != null && assignment.matches(classTeacher);
            teacherAssignmentService.assignTeacher(new TeacherAssignmentCreateRequest(
                    staffId,
                    schoolClass.getId(),
                    subject.getId(),
                    currentTermId,
                    isClassTeacher,
                    null,
                    AssignmentStatus.ACTIVE
            ));

            if (isClassTeacher) {
                classService.assignClassTeacher(schoolClass.getId(), staffId);
                classTeacherMatched = true;
            }
        }

        if (!classTeacherMatched) {
            throw new ResourceNotFoundException("class_teacher_assignment must match one of the imported teaching_assignments.");
        }
    }

    private String createStudentAndGuardiansFromRow(Map<String, String> row, UUID tenantId) {
        String admissionNumber = normalize(row.get("admission_number"));
        if (admissionNumber.isBlank()) {
            admissionNumber = identifierService.generateStudentId(tenantId);
        }

        UUID branchId = resolveBranchId(row.get("branch_code"), tenantId);
        SchoolClass schoolClass = resolveClass(required(row, "grade_level"), row.get("stream_name"), tenantId);

        StudentResponse studentResponse = studentService.enrollStudent(new StudentCreateRequest(
                admissionNumber,
                required(row, "first_name"),
                required(row, "last_name"),
                parseDate(required(row, "date_of_birth")),
                Gender.valueOf(required(row, "gender").toUpperCase(Locale.ROOT)),
                blankToNull(normalize(row.get("email"))),
                blankToNull(normalize(row.get("phone"))),
                branchId,
                schoolClass.getId(),
                StudentStatus.ACTIVE,
                blankToNull(normalize(row.get("medical_conditions")))
        ));

        Guardian guardian1 = getOrCreateGuardian(
                required(row, "guardian1_first_name"),
                required(row, "guardian1_last_name"),
                required(row, "guardian1_phone"),
                row.get("guardian1_email"),
                required(row, "guardian1_relationship"),
                tenantId
        );
        linkGuardianToStudent(studentResponse.id(), guardian1, tenantId);

        boolean hasGuardian2 = !normalize(row.get("guardian2_first_name")).isBlank()
                || !normalize(row.get("guardian2_last_name")).isBlank()
                || !normalize(row.get("guardian2_phone")).isBlank()
                || !normalize(row.get("guardian2_email")).isBlank();
        if (hasGuardian2) {
            Guardian guardian2 = getOrCreateGuardian(
                    required(row, "guardian2_first_name"),
                    required(row, "guardian2_last_name"),
                    required(row, "guardian2_phone"),
                    row.get("guardian2_email"),
                    required(row, "guardian2_relationship"),
                    tenantId
            );
            linkGuardianToStudent(studentResponse.id(), guardian2, tenantId);
        }
        return admissionNumber;
    }

    private Guardian getOrCreateGuardian(
            String firstName,
            String lastName,
            String phone,
            String email,
            String relationship,
            UUID tenantId
    ) {
        String normalizedPhone = normalize(phone);
        String normalizedEmail = blankToNull(normalize(email).toLowerCase(Locale.ROOT));

        Optional<Guardian> existingByEmail = normalizedEmail == null
                ? Optional.empty()
                : guardianRepository.findByEmailAndTenantId(normalizedEmail, tenantId);
        if (existingByEmail.isPresent()) {
            return existingByEmail.get();
        }

        Optional<Guardian> existingByPhone = guardianRepository.findByPhoneAndTenantId(normalizedPhone, tenantId);
        if (existingByPhone.isPresent()) {
            return existingByPhone.get();
        }

        guardianService.createGuardian(new GuardianCreateRequest(
                firstName,
                lastName,
                normalizedPhone,
                normalizedEmail,
                RelationshipType.valueOf(relationship.toUpperCase(Locale.ROOT)),
                true
        ));

        if (normalizedEmail != null) {
            return guardianRepository.findByEmailAndTenantId(normalizedEmail, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Guardian was created but could not be reloaded by email."));
        }
        return guardianRepository.findByPhoneAndTenantId(normalizedPhone, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Guardian was created but could not be reloaded by phone."));
    }

    private void linkGuardianToStudent(UUID studentId, Guardian guardian, UUID tenantId) {
        if (!studentGuardianRepository.existsByStudentIdAndGuardianIdAndTenantId(studentId, guardian.getId(), tenantId)) {
            guardianService.linkToStudent(guardian.getId(), studentId);
        }
    }

    private UUID resolveBranchId(String branchCode, UUID tenantId) {
        String normalizedCode = normalize(branchCode);
        if (normalizedCode.isBlank()) {
            return null;
        }
        Branch branch = branchRepository.findByCodeAndTenantId(normalizedCode, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch code not found: " + normalizedCode));
        return branch.getId();
    }

    private UUID resolveSubjectGroupId(String subjectGroupName, UUID tenantId) {
        String normalizedName = normalize(subjectGroupName);
        if (normalizedName.isBlank()) {
            return null;
        }
        var group = subjectGroupRepository.findByNameAndTenantId(normalizedName, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject group not found: " + normalizedName));
        return group.getId();
    }

    private SchoolClass resolveClass(String gradeLevel, String streamName, UUID tenantId) {
        String normalizedGrade = gradeLevel.strip();
        String normalizedStream = normalize(streamName);
        if (normalizedStream.isBlank()) {
            return schoolClassRepository.findByGradeLevelAndStreamNameIsNullAndTenantId(normalizedGrade, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found for grade " + normalizedGrade));
        }
        return schoolClassRepository.findByGradeLevelAndStreamNameAndTenantId(normalizedGrade, normalizedStream, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found for " + normalizedGrade + " / " + normalizedStream));
    }

    private ImportErrorDetail toRowError(int rowNumber, Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        return new ImportErrorDetail(rowNumber, null, message, null);
    }

    private ImportJob findJob(UUID jobId, UUID tenantId) {
        return importJobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Import job not found: " + jobId));
    }

    private String required(Map<String, String> row, String key) {
        String value = normalize(row.get(key));
        if (value.isBlank()) {
            throw new ResourceNotFoundException("Required field missing during import: " + key);
        }
        return value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private List<String> splitAssignments(String rawAssignments) {
        return Arrays.stream(rawAssignments.split(";"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    private ImportedTeachingAssignment parseAssignment(String rawAssignment) {
        String[] parts = rawAssignment.split("\\|", -1);
        if (parts.length != 3) {
            throw new ResourceNotFoundException("Teaching assignments must use Subject|Grade|Stream format. Leave stream blank as Subject|Grade| when needed.");
        }

        String subject = parts[0].trim();
        String gradeLevel = parts[1].trim();
        String streamName = parts[2].trim();

        if (subject.isBlank() || gradeLevel.isBlank()) {
            throw new ResourceNotFoundException("Teaching assignments require both subject and grade.");
        }

        return new ImportedTeachingAssignment(subject, gradeLevel, streamName);
    }

    private LocalDate parseDate(String value) {
        return LocalDate.parse(value, DATE_FMT);
    }

    private record ImportedTeachingAssignment(
            String subject,
            String gradeLevel,
            String streamName
    ) {
        private boolean matches(ImportedTeachingAssignment other) {
            return subject.equalsIgnoreCase(other.subject)
                    && gradeLevel.equalsIgnoreCase(other.gradeLevel)
                    && streamName.equalsIgnoreCase(other.streamName);
        }
    }
}
