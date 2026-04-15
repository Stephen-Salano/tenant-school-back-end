package io.psyllabus.platform.academic.service.mapper;

import io.psyllabus.platform.academic.dto.response.SubjectGroupResponse;
import io.psyllabus.platform.academic.entities.SubjectGroup;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper for converting SubjectGroup entities to DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SubjectGroupMapper {

    SubjectGroupResponse toResponse(SubjectGroup subjectGroup);
}
