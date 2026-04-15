package io.psyllabus.platform.messaging.service.mapper;

import io.psyllabus.platform.messaging.dto.response.MessageResponse;

import io.psyllabus.platform.messaging.entities.Message;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper for converting Message entities to DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MessageMapper {

    MessageResponse toResponse(Message message);

    MessageResponse toDetailResponse(Message message);
}
