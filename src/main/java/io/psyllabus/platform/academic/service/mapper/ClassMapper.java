package io.psyllabus.platform.academic.service.mapper;

import io.psyllabus.platform.academic.dto.response.ClassResponse;
import io.psyllabus.platform.academic.entities.SchoolClass;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper for converting SchoolClass entities to DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ClassMapper {

    ClassResponse toResponse(SchoolClass schoolClass);
}
