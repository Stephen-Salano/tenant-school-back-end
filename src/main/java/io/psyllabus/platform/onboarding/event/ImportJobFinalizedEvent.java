package io.psyllabus.platform.onboarding.event;

import io.psyllabus.platform.onboarding.entity.ImportJob;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Triggered when an onboarding import job finishes (DONE, COMPLETED_WITH_ERRORS, or FAILED).
 */
@Getter
public class ImportJobFinalizedEvent extends ApplicationEvent {
    private final ImportJob job;

    public ImportJobFinalizedEvent(Object source, ImportJob job) {
        super(source);
        this.job = job;
    }
}
