package io.psyllabus.platform.academic.service.mapper;

import io.psyllabus.platform.academic.dto.response.AcademicTermResponse;
import io.psyllabus.platform.academic.entities.AcademicTerm;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper for converting AcademicTerm entities to DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AcademicTermMapper {

    AcademicTermResponse toResponse(AcademicTerm academicTerm);
}
