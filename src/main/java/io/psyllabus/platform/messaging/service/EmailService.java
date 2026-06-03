package io.psyllabus.platform.messaging.service;

import com.resend.Resend;
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
     * Render the template and deliver via the platform Resend client.
     *
     * @param message the typed email payload; must not contain plaintext passwords
     */
    void send(EmailMessage message);

    /**
     * Render the template and deliver via a specific Resend client.
     * Used by the listener after resolving the correct client (platform vs tenant).
     *
     * @param message the typed email payload
     * @param client  the Resend client to use for delivery
     */
    void send(EmailMessage message, Resend client);

    /**
     * Convenience method to send an inquiry to the Psyllabus support team.
     *
     * @param from    the school admin's email address
     * @param subject the subject of the inquiry
     * @param body    the inquiry content
     */
    void sendToSupport(String from, String subject, String body);
}
