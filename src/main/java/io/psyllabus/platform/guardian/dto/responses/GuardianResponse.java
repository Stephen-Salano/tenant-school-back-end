package io.psyllabus.platform.guardian.dto.responses;

import io.psyllabus.platform.guardian.enums.RelationshipType;

import java.util.UUID;

/**
 * (single guardian basic info)
 */
public record GuardianResponse(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        String phone,
        String email,
        RelationshipType relationshipType
) {
}
