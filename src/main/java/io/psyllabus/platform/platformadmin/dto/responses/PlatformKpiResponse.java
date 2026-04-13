package io.psyllabus.platform.platformadmin.dto.responses;

/**
 * Lightweight KPI payload for platform-admin dashboard bootstrap.
 */
public record PlatformKpiResponse(
        long totalSchools,
        long activeSchools,
        long inactiveSchools,
        long queuedImportJobs,
        long processingImportJobs,
        long failedImportJobs
) {
}
