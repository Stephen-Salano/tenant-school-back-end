package io.psyllabus.platform.onboarding.listener;

import io.psyllabus.platform.common.config.RabbitMQConstants;
import io.psyllabus.platform.onboarding.event.OnboardingJobQueuedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OnboardingJobMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJobQueued(OnboardingJobQueuedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.ONBOARDING_EXCHANGE,
                event.getRoutingKey(),
                new OnboardingJobPayload(event.getJobId(), event.getTenantId())
        );
        log.info("Published onboarding job to RabbitMQ [jobId={}, routingKey={}]", 
                event.getJobId(), event.getRoutingKey());
    }
}