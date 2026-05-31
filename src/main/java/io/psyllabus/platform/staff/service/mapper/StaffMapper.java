package io.psyllabus.platform.staff.service.mapper;

import io.psyllabus.platform.staff.dto.responses.StaffDetailResponse;
import io.psyllabus.platform.staff.dto.responses.StaffResponse;
import io.psyllabus.platform.staff.entities.Staff;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper for converting Staff entities to DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StaffMapper {

    StaffResponse toResponse(Staff staff);

    @Mapping(target = "subjectGroupName", ignore = true)
    StaffDetailResponse toDetailResponse(Staff staff);
}
