package io.psyllabus.platform.branch.service.mapper;

import io.psyllabus.platform.branch.dto.responses.BranchDetailResponse;
import io.psyllabus.platform.branch.dto.responses.BranchResponse;
import io.psyllabus.platform.branch.entities.Branch;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper for converting Branch entities to DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BranchMapper {

    @Mapping(target = "branchAdminName", source = "adminName")
    BranchResponse toResponse(Branch branch, String adminName);

    @Mapping(target = "branchAdminName", source = "adminName")
    BranchDetailResponse toDetailResponse(Branch branch, String adminName);
}
