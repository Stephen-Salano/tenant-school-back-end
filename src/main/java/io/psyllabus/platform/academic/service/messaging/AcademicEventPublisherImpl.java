package io.psyllabus.platform.academic.service.messaging;

import io.psyllabus.platform.academic.dto.events.CurriculumSelectedEvent;
import io.psyllabus.platform.common.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link AcademicEventPublisher} using RabbitMQ.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AcademicEventPublisherImpl implements AcademicEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishCurriculumSelected(CurriculumSelectedEvent event) {
        log.info("Publishing curriculum selected event for tenant: {}", event.tenantId());
        
        String routingKey = "academic.event.curriculum-selected";
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.ACADEMIC_EXCHANGE,
            routingKey,
            event
        );
    }
}
