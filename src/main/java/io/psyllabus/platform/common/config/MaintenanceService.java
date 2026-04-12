package io.psyllabus.platform.common.config;

import io.psyllabus.platform.onboarding.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled maintenance tasks that run on a fixed cadence in non-test environments.
 *
 * <p>All methods here are operational housekeeping — they do not implement business
 * logic and must never be called from service code directly. Add new scheduled
 * tasks here rather than scattering {@code @Scheduled} across feature packages.
 *
 * <p>This bean is excluded from the {@code test} profile to avoid scheduling
 * interference with unit and integration tests.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class MaintenanceService {

    /**
     * How many hours to retain {@code raw_data} JSONB blobs on ImportJob records
     * before nullifying them. 24 hours gives the admin time to download the
     * error report before the raw data is cleared.
     */
    private static final int RAW_DATA_RETENTION_HOURS = 24;

    private final ImportJobRepository importJobRepository;

    /**
     * Nullifies {@code raw_data} on ImportJob records that reached a terminal state
     * more than {@value #RAW_DATA_RETENTION_HOURS} hours ago.
     *
     * <p>This implements the K-DPA data minimisation principle: once an import job
     * is complete, retaining the full CSV row data (which contains student PII)
     * beyond the review window is unnecessary.
     *
     * <p>Runs at 02:00 AM server time daily.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void clearStaleImportRawData() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(RAW_DATA_RETENTION_HOURS);
        int cleared = importJobRepository.clearStaleRawData(cutoff);

        if (cleared > 0) {
            log.info("Maintenance: nullified raw_data on {} completed ImportJob record(s) " +
                    "older than {} hours (K-DPA data minimisation)", cleared, RAW_DATA_RETENTION_HOURS);
        } else {
            log.debug("Maintenance: no stale ImportJob raw_data to clear");
        }
    }
}
