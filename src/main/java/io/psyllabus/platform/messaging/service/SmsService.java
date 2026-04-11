package io.psyllabus.platform.messaging.service;

import io.psyllabus.platform.messaging.dto.SmsMessage;

/**
 * Sends operational SMS messages via Africa's Talking.
 *
 * <p>Not called during bulk import — the pipeline skips SMS to avoid costs.
 * Called only for operational events (fee alerts, results, etc.).
 */
public interface SmsService {
    /**
     * Render the template and deliver via Africa's Talking.
     *
     * @param message the typed SMS payload
     */
    void send(SmsMessage message);
}
