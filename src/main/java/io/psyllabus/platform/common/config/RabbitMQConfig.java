package io.psyllabus.platform.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.retry.support.RetryTemplate;

import static io.psyllabus.platform.common.config.RabbitMQConstants.*;

/**
 * Psyllabus RabbitMQ Topology.
 * Orchestrates cross-cutting events: Academic Term Generation, Bulk Onboarding, and Notifications.
 */
@Configuration
@Slf4j
public class RabbitMQConfig {

    private static final String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    private static final String X_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);

        // ── Retry: 3 attempts, exponential backoff (1s → 2s → 4s) ──
        RetryTemplate retry = RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(1_000, 2.0, 4_000)
                .build();
        factory.setRetryTemplate(retry);

        factory.setDefaultRequeueRejected(false); // Move to DLQ on failure
        factory.setTaskExecutor(new SimpleAsyncTaskExecutor()); // Virtual Threads
        
        log.info("RabbitMQ listener factory configured [retry=3, backoff=1s/2s/4s, requeue=false]");
        return factory;
    }

    // ══════════════════════════════════════════════════════════════════════
    // EXCHANGES
    // ══════════════════════════════════════════════════════════════════════

    @Bean
    public TopicExchange academicExchange() {
        return ExchangeBuilder.topicExchange(ACADEMIC_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange onboardingExchange() {
        return ExchangeBuilder.directExchange(ONBOARDING_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange notificationsExchange() {
        return ExchangeBuilder.topicExchange(NOTIFICATIONS_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange academicDLX() {
        return ExchangeBuilder.directExchange(ACADEMIC_DLX).durable(true).build();
    }

    @Bean
    public DirectExchange onboardingDLX() {
        return ExchangeBuilder.directExchange(ONBOARDING_DLX).durable(true).build();
    }

    @Bean
    public DirectExchange notificationsDLX() {
        return ExchangeBuilder.directExchange(NOTIFICATIONS_DLX).durable(true).build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // QUEUES & BINDINGS: ACADEMIC (SM3)
    // ══════════════════════════════════════════════════════════════════════

    @Bean
    public Queue termDraftingQueue() {
        return QueueBuilder.durable(TERM_DRAFTING_QUEUE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, ACADEMIC_DLX)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, TERM_DRAFTING_DLQ)
                .build();
    }

    @Bean
    public Queue termDraftingDLQ() {
        return new Queue(TERM_DRAFTING_DLQ);
    }

    @Bean
    public Binding termDraftingBinding() {
        return BindingBuilder.bind(termDraftingQueue()).to(academicExchange()).with(TERM_DRAFTING_ROUTING_KEY);
    }

    @Bean
    public Binding termDraftingDlqBinding() {
        return BindingBuilder.bind(termDraftingDLQ()).to(academicDLX()).with(TERM_DRAFTING_DLQ);
    }

    // ══════════════════════════════════════════════════════════════════════
    // QUEUES & BINDINGS: ONBOARDING (SM4)
    // ══════════════════════════════════════════════════════════════════════

    @Bean
    public Queue studentImportQueue() {
        return QueueBuilder.durable(STUDENT_IMPORT_QUEUE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, ONBOARDING_DLX)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, STUDENT_IMPORT_DLQ)
                .build();
    }

    @Bean
    public Queue studentImportDLQ() {
        return new Queue(STUDENT_IMPORT_DLQ);
    }

    @Bean
    public Binding studentImportBinding() {
        return BindingBuilder.bind(studentImportQueue()).to(onboardingExchange()).with(STUDENT_IMPORT_ROUTING_KEY);
    }

    @Bean
    public Binding studentImportDlqBinding() {
        return BindingBuilder.bind(studentImportDLQ()).to(onboardingDLX()).with(STUDENT_IMPORT_DLQ);
    }

    @Bean
    public Queue staffImportQueue() {
        return QueueBuilder.durable(STAFF_IMPORT_QUEUE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, ONBOARDING_DLX)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, STAFF_IMPORT_DLQ)
                .build();
    }

    @Bean
    public Queue staffImportDLQ() {
        return new Queue(STAFF_IMPORT_DLQ);
    }

    @Bean
    public Binding staffImportBinding() {
        return BindingBuilder.bind(staffImportQueue()).to(onboardingExchange()).with(STAFF_IMPORT_ROUTING_KEY);
    }

    @Bean
    public Binding staffImportDlqBinding() {
        return BindingBuilder.bind(staffImportDLQ()).to(onboardingDLX()).with(STAFF_IMPORT_DLQ);
    }

    // ══════════════════════════════════════════════════════════════════════
    // QUEUES & BINDINGS: NOTIFICATIONS (SMS/Email)
    // ══════════════════════════════════════════════════════════════════════

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, NOTIFICATIONS_DLX)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, EMAIL_DLQ)
                .build();
    }

    @Bean
    public Queue emailDLQ() {
        return new Queue(EMAIL_DLQ);
    }

    @Bean
    public Binding emailBinding() {
        return BindingBuilder.bind(emailQueue()).to(notificationsExchange()).with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding emailDlqBinding() {
        return BindingBuilder.bind(emailDLQ()).to(notificationsDLX()).with(EMAIL_DLQ);
    }

    @Bean
    public Queue smsQueue() {
        return QueueBuilder.durable(SMS_QUEUE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, NOTIFICATIONS_DLX)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, SMS_DLQ)
                .build();
    }

    @Bean
    public Queue smsDLQ() {
        return new Queue(SMS_DLQ);
    }

    @Bean
    public Binding smsBinding() {
        return BindingBuilder.bind(smsQueue()).to(notificationsExchange()).with(SMS_ROUTING_KEY);
    }

    @Bean
    public Binding smsDlqBinding() {
        return BindingBuilder.bind(smsDLQ()).to(notificationsDLX()).with(SMS_DLQ);
    }
}
