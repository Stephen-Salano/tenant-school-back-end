package io.psyllabus.platform.academic.service.mapper;

import io.psyllabus.platform.academic.dto.response.SubjectCategoryResponse;
import io.psyllabus.platform.academic.entities.SubjectCategory;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper for converting SubjectCategory entities to DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SubjectCategoryMapper {

    SubjectCategoryResponse toResponse(SubjectCategory subjectCategory);
}
