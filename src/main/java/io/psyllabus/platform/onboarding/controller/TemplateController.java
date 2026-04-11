package io.psyllabus.platform.onboarding.controller;

import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.onboarding.service.CsvTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TemplateController — Facade for serving bulk import CSV templates.
 * 
 * <p>Delegates to {@link CsvTemplateService}. Extends {@link BaseController}
 * for multi-tenant context and user resolution.</p>
 * 
 * <p>Only School Admins can access these templates for data ingestion.</p>
 */
@RestController
@RequestMapping("/api/onboarding/templates")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Onboarding Templates", description = "Standardized CSV templates for bulk student and staff ingestion.")
public class TemplateController extends BaseController {

    private final CsvTemplateService templateService;

    private static final String STUDENTS_TEMPLATE = "students_import_template.csv";
    private static final String STAFF_TEMPLATE = "staff_import_template.csv";

    /**
     * GET /api/onboarding/templates/students — Admin only.
     * Serves the standardized student import CSV template.
     */
    @GetMapping("/students")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Download student bulk import CSV template")
    public ResponseEntity<Resource> downloadStudentsTemplate(Authentication authentication) {
        log.debug("Requesting student import template download");
        return serveFile(STUDENTS_TEMPLATE, authentication);
    }

    /**
     * GET /api/onboarding/templates/staff — Admin only.
     * Serves the standardized staff import CSV template.
     */
    @GetMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Download staff bulk import CSV template")
    public ResponseEntity<Resource> downloadStaffTemplate(Authentication authentication) {
        log.debug("Requesting staff import template download");
        return serveFile(STAFF_TEMPLATE, authentication);
    }

    /**
     * Serves the resource as a raw binary stream to allow direct download.
     * Exceptions are handled by the GlobalExceptionHandler to return standard ApiResponse JSON on failure.
     */
    private ResponseEntity<Resource> serveFile(String filename, Authentication authentication) {
        Resource resource = templateService.getTemplate(
                filename, 
                getCurrentTenantId(), 
                getCurrentUser(authentication)
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }
}
