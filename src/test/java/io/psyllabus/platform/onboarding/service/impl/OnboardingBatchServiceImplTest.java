package io.psyllabus.platform.onboarding.service.impl;

import io.psyllabus.platform.academic.repository.AcademicTermRepository;
import io.psyllabus.platform.academic.repository.SchoolClassRepository;
import io.psyllabus.platform.academic.repository.SubjectGroupRepository;
import io.psyllabus.platform.academic.repository.SubjectRepository;
import io.psyllabus.platform.academic.service.ClassService;
import io.psyllabus.platform.academic.service.TeacherAssignmentService;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.branch.repository.BranchRepository;
import io.psyllabus.platform.common.exception.ValidationException;
import io.psyllabus.platform.common.service.IdentifierService;
import io.psyllabus.platform.guardian.dto.request.GuardianCreateRequest;
import io.psyllabus.platform.guardian.dto.responses.GuardianResponse;
import io.psyllabus.platform.guardian.entities.Guardian;
import io.psyllabus.platform.guardian.enums.RelationshipType;
import io.psyllabus.platform.guardian.repository.GuardianRepository;
import io.psyllabus.platform.guardian.repository.StudentGuardianRepository;
import io.psyllabus.platform.guardian.services.GuardianService;
import io.psyllabus.platform.onboarding.repository.ImportBatchOutcomeRepository;
import io.psyllabus.platform.onboarding.repository.ImportJobRepository;
import io.psyllabus.platform.onboarding.service.OnboardingBatchService;
import io.psyllabus.platform.staff.services.StaffService;
import io.psyllabus.platform.student.service.StudentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingBatchServiceImplTest {

    @Mock private ImportJobRepository importJobRepository;
    @Mock private StaffService staffService;
    @Mock private StudentService studentService;
    @Mock private GuardianService guardianService;
    @Mock private TeacherAssignmentService teacherAssignmentService;
    @Mock private ClassService classService;
    @Mock private GuardianRepository guardianRepository;
    @Mock private StudentGuardianRepository studentGuardianRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private SchoolClassRepository schoolClassRepository;
    @Mock private SubjectGroupRepository subjectGroupRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private AcademicTermRepository academicTermRepository;
    @Mock private IdentifierService identifierService;
    @Mock private AuditService auditService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ImportBatchOutcomeRepository outcomeRepository;
    @Mock private OnboardingBatchService self;

    @InjectMocks
    private OnboardingBatchServiceImpl onboardingBatchService;

    @Test
    void getOrCreateGuardian_throwsValidationExceptionOnNameMismatch() {
        UUID tenantId = UUID.randomUUID();
        String phone = "0712345678";
        
        Guardian existing = new Guardian();
        existing.setFirstName("John");
        existing.setLastName("Doe");
        existing.setPhone(phone);

        when(guardianRepository.findByPhoneAndTenantId(phone, tenantId)).thenReturn(Optional.of(existing));

        ValidationException ex = assertThrows(ValidationException.class, () -> 
            ReflectionTestUtils.invokeMethod(onboardingBatchService, "getOrCreateGuardian",
                "Jane", "Smith", phone, null, "MOTHER", tenantId)
        );

        assertTrue(ex.getMessage().contains("already registered to another person"));
        assertTrue(ex.getMessage().contains("John Doe"));
    }

    @Test
    void getOrCreateGuardian_returnsExistingOnNameMatch() {
        UUID tenantId = UUID.randomUUID();
        String phone = "0712345678";
        
        Guardian existing = new Guardian();
        existing.setFirstName("John");
        existing.setLastName("Doe");
        existing.setPhone(phone);

        when(guardianRepository.findByPhoneAndTenantId(phone, tenantId)).thenReturn(Optional.of(existing));

        Guardian result = ReflectionTestUtils.invokeMethod(onboardingBatchService, "getOrCreateGuardian",
                "john", "DOE", phone, null, "FATHER", tenantId);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
    }

    @Test
    void getOrCreateGuardian_createsNewWhenNotFound() {
        UUID tenantId = UUID.randomUUID();
        UUID guardianId = UUID.randomUUID();
        String phone = "0712345678";

        GuardianResponse response = new GuardianResponse(
                guardianId, UUID.randomUUID(), "New", "Guardian", phone, "new@guard.com", RelationshipType.OTHER
        );
        when(guardianService.createGuardian(any(GuardianCreateRequest.class))).thenReturn(response);

        Guardian created = new Guardian();
        created.setId(guardianId);
        when(guardianRepository.findByIdAndTenantId(guardianId, tenantId)).thenReturn(Optional.of(created));

        Guardian result = ReflectionTestUtils.invokeMethod(onboardingBatchService, "getOrCreateGuardian",
                "New", "Guardian", phone, "new@guard.com", "OTHER", tenantId);

        assertNotNull(result);
        assertEquals(guardianId, result.getId());
    }
}
