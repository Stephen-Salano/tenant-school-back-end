package io.psyllabus.platform.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

/**
 * Email rendering configuration using Thymeleaf.
 *
 * <p>Actual delivery is handled by Resend via {@code ResendEmailServiceImpl}.
 * Spring Mail / SMTP is not used — we use the Resend SDK for efficiency.
 *
 * <p>Rate limiting: emails are consumed from {@code psyllabus.notifications.email}
 * one at a time by {@link io.psyllabus.platform.messaging.listener.EmailNotificationListener}.
 * The RabbitMQ queue is the natural throttle.
 */
@Configuration
@Slf4j
public class EmailConfig {

    /**
     * Resolves Thymeleaf templates from {@code resources/templates/emails/}.
     */
    @Bean
    @Description("Thymeleaf template resolver for email HTML templates")
    public ITemplateResolver emailTemplateResolver() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/emails/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false); // Enable caching in production if templates are stable
        log.info("Email template resolver configured [prefix=templates/emails/, suffix=.html]");
        return resolver;
    }

    /**
     * Template engine wired to the email resolver.
     */
    @Bean
    @Description("Thymeleaf template engine for email HTML rendering")
    public SpringTemplateEngine emailTemplateEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.addTemplateResolver(emailTemplateResolver());
        log.info("Email template engine configured");
        return engine;
    }
}
