package io.psyllabus.platform.audit.controller;

import io.psyllabus.platform.audit.dto.request.AuditExportRequest;
import io.psyllabus.platform.audit.dto.response.AuditExportJobResponse;
import io.psyllabus.platform.audit.enums.AuditExportStatus;
import io.psyllabus.platform.audit.service.AuditExportService;
import io.psyllabus.platform.audit.service.AuditQueryService;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.psyllabus.platform.platformadmin.entities.PlatformAdmin;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuditControllerTest {

    @Test
    void export_returnsJobResponseForPlatformAdmin() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            AuditController controller = context.getBean(AuditController.class);
            AuditExportService auditExportService = context.getBean(AuditExportService.class);

            UUID adminId = UUID.randomUUID();
            UUID jobId = UUID.randomUUID();
            AuditExportRequest request = new AuditExportRequest(null, "STUDENT", null, null, null, null);
            Authentication authentication = platformAdminAuthentication(adminId);

            when(auditExportService.queueExport(any(AuditExportRequest.class), eq(adminId)))
                    .thenReturn(new AuditExportJobResponse(jobId, AuditExportStatus.QUEUED, LocalDateTime.now()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            try {
                ResponseEntity<ApiResponse<AuditExportJobResponse>> response = controller.export(request, authentication);

                assertEquals(HttpStatus.CREATED, response.getStatusCode());
                assertEquals(jobId, response.getBody().data().jobId());
                assertEquals(AuditExportStatus.QUEUED, response.getBody().data().status());
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }

    @Test
    void export_rejectsNonPlatformRoles() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            AuditController controller = context.getBean(AuditController.class);
            AuditExportService auditExportService = context.getBean(AuditExportService.class);

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    "school-admin",
                    null,
                    List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            try {
                assertThrows(
                        AccessDeniedException.class,
                        () -> controller.export(new AuditExportRequest(null, "STUDENT", null, null, null, null), authentication)
                );
                verifyNoInteractions(auditExportService);
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }

    private Authentication platformAdminAuthentication(UUID adminId) {
        PlatformAdmin admin = PlatformAdmin.builder()
                .id(adminId)
                .username("platform-admin")
                .email("platform@example.com")
                .hashedPassword("hash")
                .firstName("Platform")
                .lastName("Admin")
                .build();

        return new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities());
    }

    @Configuration
    @EnableMethodSecurity
    static class TestConfig {

        @Bean
        AuditController auditController(AuditQueryService auditQueryService, AuditExportService auditExportService) {
            return new AuditController(auditQueryService, auditExportService);
        }

        @Bean
        AuditQueryService auditQueryService() {
            return mock(AuditQueryService.class);
        }

        @Bean
        AuditExportService auditExportService() {
            return mock(AuditExportService.class);
        }
    }
}
