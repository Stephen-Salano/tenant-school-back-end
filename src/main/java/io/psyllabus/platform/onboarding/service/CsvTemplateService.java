package io.psyllabus.platform.onboarding.service;

import io.psyllabus.platform.auth.entities.User;
import org.springframework.core.io.Resource;
import java.util.UUID;

/**
 * Service for serving static CSV templates for bulk data ingestion.
 * Handles the logic of finding resources and auditing the download activity.
 */
public interface CsvTemplateService {

    /**
     * Retrieves a CSV template resource by filename and logs the audit event.
     *
     * @param filename the name of the template file in the classpath
     * @param tenantId the school (tenant) requesting the template
     * @param actor    the admin user initiating the download
     * @return the template as a Spring Resource
     * @throws io.psyllabus.platform.common.exception.ResourceNotFoundException if file doesn't exist
     */
    Resource getTemplate(String filename, UUID tenantId, User actor);
}
