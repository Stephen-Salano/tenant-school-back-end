package io.psyllabus.platform.onboarding.listener;

import java.util.UUID;

/**
 * RabbitMQ payload for onboarding jobs.
 */
public record OnboardingJobPayload(
        UUID jobId
) {
}
