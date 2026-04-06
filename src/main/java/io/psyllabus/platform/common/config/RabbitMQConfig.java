package io.psyllabus.platform.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Foundation for Psyllabus Messaging.
 * Uses a Topic Exchange to allow flexible routing of academic, onboarding, and notification events.
 */
@Configuration
public class RabbitMQConfig {

    public static final String ACADEMIC_EXCHANGE = "psyllabus.academic";
    public static final String TERM_DRAFTING_QUEUE = "psyllabus.academic.term-drafting";
    public static final String ACADEMIC_DLX = "psyllabus.academic.dlx";
    public static final String TERM_DRAFTING_DLQ = "psyllabus.academic.term-drafting.dlq";

    @Bean
    public TopicExchange academicExchange() {
        return new TopicExchange(ACADEMIC_EXCHANGE);
    }

    @Bean
    public TopicExchange academicDeadLetterExchange() {
        return new TopicExchange(ACADEMIC_DLX);
    }

    @Bean
    public Queue termDraftingQueue() {
        return QueueBuilder.durable(TERM_DRAFTING_QUEUE)
                .withArgument("x-dead-letter-exchange", ACADEMIC_DLX)
                .withArgument("x-dead-letter-routing-key", "academic.event.term-drafting.failed")
                .build();
    }

    @Bean
    public Queue termDraftingDeadLetterQueue() {
        return new Queue(TERM_DRAFTING_DLQ);
    }

    @Bean
    public Binding termDraftingBinding(Queue termDraftingQueue, TopicExchange academicExchange) {
        return BindingBuilder.bind(termDraftingQueue).to(academicExchange).with("academic.event.#");
    }

    @Bean
    public Binding termDraftingDeadLetterBinding(Queue termDraftingDeadLetterQueue, TopicExchange academicDeadLetterExchange) {
        return BindingBuilder.bind(termDraftingDeadLetterQueue).to(academicDeadLetterExchange).with("academic.event.#");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
