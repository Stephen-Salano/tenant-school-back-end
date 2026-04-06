package io.psyllabus.platform.academic.service.messaging;

import io.psyllabus.platform.academic.dto.events.CurriculumSelectedEvent;

/**
 * Interface for publishing academic domain events.
 */
public interface AcademicEventPublisher {
    /**
     * Publishes an event when a school's curriculum is selected.
     * @param event The curriculum selection details.
     */
    void publishCurriculumSelected(CurriculumSelectedEvent event);
}
