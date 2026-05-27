package io.psyllabus.platform.onboarding.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class OnboardingJobQueuedEvent extends ApplicationEvent {
    private final UUID jobId;
    private final UUID tenantId;
    private final String routingKey;

    public OnboardingJobQueuedEvent(Object source, UUID jobId, UUID tenantId, String routingKey) {
        super(source);
        this.jobId = jobId;
        this.tenantId = tenantId;
        this.routingKey = routingKey;
    }
}