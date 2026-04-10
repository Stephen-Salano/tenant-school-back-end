package io.psyllabus.platform.messaging.service;

import io.psyllabus.platform.messaging.enums.EmailType;

/**
 * Service for sending transactional and system emails.
 */
public interface EmailService {
    /**
     * Send an email with a specific categorization.
     *
     * @param to      the recipient's email address
     * @param subject the email subject
     * @param htmlBody the HTML content of the email
     * @param type    the category of the email being sent
     */
    void send(String to, String subject, String htmlBody, EmailType type);

    /**
     * Convenience method to send an email to the Psyllabus support team.
     *
     * @param from    the school admin's email address
     * @param subject the subject of the inquiry
     * @param body    the inquiry content
     */
    void sendToSupport(String from, String subject, String body);
}
