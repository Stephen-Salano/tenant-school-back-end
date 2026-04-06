package io.psyllabus.platform.academic.dto.events;

import io.psyllabus.platform.school.enums.CurriculumType;
import java.util.UUID;

/**
 * Event published when a school selects or changes their curriculum type.
 * Triggers the proactive drafting of academic terms.
 */
public record CurriculumSelectedEvent(
    UUID tenantId,
    CurriculumType curriculumType,
    int startYear
) {}
