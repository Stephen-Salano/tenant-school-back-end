package io.psyllabus.platform.student.service;

import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.auth.dto.request.RegisterRequest;
import io.psyllabus.platform.auth.dto.responses.UserResponse;
import io.psyllabus.platform.auth.enums.UserType;
import io.psyllabus.platform.auth.services.AuthService;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.student.dto.request.StudentCreateRequest;
import io.psyllabus.platform.student.dto.responses.StudentResponse;
import io.psyllabus.platform.student.entities.Student;
import io.psyllabus.platform.student.enums.Gender;
import io.psyllabus.platform.student.enums.StudentStatus;
import io.psyllabus.platform.student.repository.StudentRepository;
import io.psyllabus.platform.student.service.mapper.StudentMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StudentServiceImplTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void enrollStudent_generatesPlaceholderEmailWhenEmailIsBlank() {
        StudentRepository studentRepository = mock(StudentRepository.class);
        AuthService authService = mock(AuthService.class);
        StudentMapper studentMapper = mock(StudentMapper.class);
        AuditService auditService = mock(AuditService.class);

        StudentServiceImpl service = new StudentServiceImpl(studentRepository, authService, studentMapper, auditService);

        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);

        StudentCreateRequest request = new StudentCreateRequest(
                "ADM12345",
                "Thomas",
                "Wambua",
                LocalDate.of(2010, 12, 1),
                Gender.MALE,
                null,
                "+254722300001",
                null,
                classId,
                StudentStatus.ACTIVE,
                null
        );

        Student student = new Student();
        student.setId(UUID.randomUUID());

        when(studentRepository.existsByAdmissionNumberAndTenantId("ADM12345", tenantId)).thenReturn(false);
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new UserResponse(userId, "ADM12345", null, "Thomas", "Wambua", UserType.STUDENT));
        when(studentMapper.toEntity(request)).thenReturn(student);
        when(studentRepository.save(student)).thenReturn(student);
        when(studentMapper.toResponse(student)).thenReturn(
                new StudentResponse(student.getId(), userId, "ADM12345", "Thomas", "Wambua", StudentStatus.ACTIVE)
        );

        service.enrollStudent(request);

        ArgumentCaptor<RegisterRequest> captor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(authService).register(captor.capture());

        assertEquals(
                "adm12345+" + tenantId + "@noemail.psyllabus.internal",
                captor.getValue().email()
        );
    }
}
