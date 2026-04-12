package io.psyllabus.platform.onboarding.service.impl;

import io.psyllabus.platform.onboarding.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled cleanup for stale import data.
 * Prevents JSONB bloat in the database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImportJobCleanupService {

    private final ImportJobRepository importJobRepository;

    /**
     * Clears raw_data from completed/failed import jobs older than 24 hours.
     * Runs at 2 AM daily.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void clearStaleImportData() {
        log.info("Starting stale import data cleanup...");
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        int cleared = importJobRepository.clearStaleRawData(cutoff);
        log.info("Cleanup complete: cleared raw_data from {} jobs", cleared);
    }
}
