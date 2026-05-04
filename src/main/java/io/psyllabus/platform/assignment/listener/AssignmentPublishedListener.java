package io.psyllabus.platform.assignment.listener;

import io.psyllabus.platform.assignment.event.AssignmentPublishedEvent;
import io.psyllabus.platform.assignment.service.AssignmentService;
import io.psyllabus.platform.common.annotation.SystemProcess;
import io.psyllabus.platform.common.config.RabbitMQConstants;
import io.psyllabus.platform.common.service.TenantExecutionService;
import io.psyllabus.platform.messaging.entities.ProcessedMessage;
import io.psyllabus.platform.messaging.repository.ProcessedMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@SystemProcess
public class AssignmentPublishedListener {

    private final TenantExecutionService tenantExecutionService;
    private final AssignmentService assignmentService;
    private final ProcessedMessageRepository processedMessageRepository;

    @RabbitListener(
            queues = RabbitMQConstants.ASSIGNMENT_PUBLISHED_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    @Transactional
    public void handle(AssignmentPublishedEvent event) {
        tenantExecutionService.runInTenant(event.tenantId(), () -> process(event));
    }

    private void process(AssignmentPublishedEvent event) {
        if (processedMessageRepository.existsById(event.idempotencyKey())) {
            log.info("Skipping duplicate assignment publication event [key={}]", event.idempotencyKey());
            return;
        }

        assignmentService.createPendingSubmissions(event.assignmentId());

        processedMessageRepository.save(ProcessedMessage.builder()
                .idempotencyKey(event.idempotencyKey())
                .tenantId(event.tenantId())
                .messageType("ASSIGNMENT_PUBLISHED")
                .build());
    }
}
