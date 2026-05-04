package io.psyllabus.platform.subscription.mapper;

import io.psyllabus.platform.subscription.dto.response.SubscriptionResponse;
import io.psyllabus.platform.subscription.dto.response.SubscriptionDetailResponse;
import io.psyllabus.platform.subscription.entity.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper for converting Subscription entities to DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SubscriptionMapper {

    SubscriptionResponse toResponse(Subscription subscription);

    SubscriptionDetailResponse toDetailResponse(Subscription subscription);
}
