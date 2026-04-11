package io.psyllabus.platform.messaging.service;

import io.psyllabus.platform.messaging.dto.EmailMessage;

/**
 * Sends transactional emails via Resend.
 *
 * <p>Callers publish an {@link EmailMessage} to the RabbitMQ queue.
 * The listener picks it up and delegates here — never call this directly
 * from a controller or import listener.
 */
public interface EmailService {
    /**
     * Render the template and deliver via Resend.
     *
     * @param message the typed email payload; must not contain plaintext passwords
     */
    void send(EmailMessage message);

    /**
     * Convenience method to send an inquiry to the Psyllabus support team.
     *
     * @param from    the school admin's email address
     * @param subject the subject of the inquiry
     * @param body    the inquiry content
     */
    void sendToSupport(String from, String subject, String body);
}
