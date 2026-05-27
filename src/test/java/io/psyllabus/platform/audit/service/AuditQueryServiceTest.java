package io.psyllabus.platform.audit.service;

import io.psyllabus.platform.audit.dto.request.AuditLogQueryRequest;
import io.psyllabus.platform.audit.dto.response.AuditLogResponse;
import io.psyllabus.platform.audit.entitiy.AuditLog;
import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.repository.AuditLogRepository;
import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.auth.enums.AccessScope;
import io.psyllabus.platform.auth.enums.UserType;
import io.psyllabus.platform.auth.repository.UserRepository;
import io.psyllabus.platform.common.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditQueryServiceTest {

    @Test
    void listLogs_returnsPagedAuditResponsesForPlatformAdmin() {
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AuditQueryService service = new AuditQueryService(auditLogRepository, userRepository);

        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        AuditLog log = AuditLog.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .userId(userId)
                .action(AuditAction.READ)
                .entityType("STUDENT")
                .entityId(UUID.randomUUID())
                .details("Viewed student")
                .ipAddress("127.0.0.1")
                .createdAt(LocalDateTime.now())
                .build();

        User user = User.builder()
                .id(userId)
                .tenantId(tenantId)
                .username("jane")
                .email("jane@example.com")
                .hashedPassword("hash")
                .firstName("Jane")
                .lastName("Doe")
                .userType(UserType.ADMIN)
                .build();

        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));
        when(userRepository.findAllById(List.of(userId))).thenReturn(List.of(user));

        PageResponse<AuditLogResponse> response = service.listLogs(
                new AuditLogQueryRequest(AuditAction.READ, "STUDENT", null, userId, null, null, 0, 20),
                UUID.randomUUID(),
                UserType.PLATFORM_ADMIN,
                AccessScope.PLATFORM
        );

        assertEquals(1, response.content().size());
        assertEquals("Jane Doe", response.content().getFirst().userName());
        assertEquals(1, response.totalElements());
        assertEquals(0, response.page());
    }
}
