package io.psyllabus.platform.platformadmin.dto.responses;

/**
 * Lightweight KPI payload for platform-admin dashboard bootstrap.
 */
public record PlatformKpiResponse(
        long totalSchools,
        long activeSchools,
        long suspendedSchools,
        long deactivatedSchools,
        long queuedImportJobs,
        long processingImportJobs,
        long failedImportJobs
) {
}
