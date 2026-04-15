package io.psyllabus.platform.academic.service.mapper;

import io.psyllabus.platform.academic.dto.response.AcademicYearResponse;
import io.psyllabus.platform.academic.entities.AcademicYear;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper for converting AcademicYear entities to DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AcademicYearMapper {

    AcademicYearResponse toResponse(AcademicYear academicYear);
}
