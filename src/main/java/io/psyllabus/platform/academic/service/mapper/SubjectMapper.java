package io.psyllabus.platform.academic.service.mapper;

import io.psyllabus.platform.academic.dto.response.SubjectResponse;
import io.psyllabus.platform.academic.entities.Subject;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper for converting Subject entities to DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SubjectMapper {

    SubjectResponse toResponse(Subject subject);
}
