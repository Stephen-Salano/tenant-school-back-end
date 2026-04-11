package io.psyllabus.platform.onboarding.service.impl;

import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.onboarding.service.CsvTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * CsvTemplateServiceImpl - Service implementation for serving CSV templates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CsvTemplateServiceImpl implements CsvTemplateService {

    private final AuditService auditService;
    private static final String CSV_PATH = "templates/csv/";

    @Override
    public Resource getTemplate(String filename, UUID tenantId, User actor) {
        log.debug("CSV template lookup [file={}, tenant={}, actor={}]", filename, tenantId, actor.getEmail());
        Resource resource = new ClassPathResource(CSV_PATH + filename);

        if (!resource.exists()) {
            log.error("CSV template missing from classpath [file={}]", filename);
            throw new ResourceNotFoundException("CSV template not found: " + filename);
        }

        // Auditing the read operation as it's sensitive in a multi-tenant environment.
        auditService.log(
                AuditAction.TEMPLATE_DOWNLOAD,
                "ONBOARDING",
                null, // No specific entity affected, it's a global template
                String.format("Admin %s downloaded template: %s", actor.getEmail(), filename)
        );

        return resource;
    }
}
