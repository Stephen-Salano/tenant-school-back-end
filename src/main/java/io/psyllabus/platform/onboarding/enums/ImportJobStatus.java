package io.psyllabus.platform.onboarding.enums;

/**
 * Lifecycle states for a bulk import job.
 *
 * <p>Valid transitions:
 * <pre>
 * QUEUED → PROCESSING → DONE
 * → COMPLETED_WITH_ERRORS
 * → FAILED
 * </pre>
 */
public enum ImportJobStatus {
    /** Job received and queued; not yet picked up by a listener. */
    QUEUED,
    /** Listener is actively processing batches. */
    PROCESSING,
    /** All rows processed successfully. */
    DONE,
    /** Processing finished but one or more rows failed validation or persistence. */
    COMPLETED_WITH_ERRORS,
    /** Fatal error; job cannot continue. Check errorDetails. */
    FAILED
}
