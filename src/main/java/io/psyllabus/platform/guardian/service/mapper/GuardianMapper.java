package io.psyllabus.platform.guardian.service.mapper;

import io.psyllabus.platform.guardian.dto.responses.GuardianResponse;
import io.psyllabus.platform.guardian.dto.responses.GuardianDetailResponse;
import io.psyllabus.platform.guardian.entities.Guardian;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper for converting Guardian entities to DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface GuardianMapper {

    GuardianResponse toResponse(Guardian guardian);

    GuardianDetailResponse toDetailResponse(Guardian guardian);
}
