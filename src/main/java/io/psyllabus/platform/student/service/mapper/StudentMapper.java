package io.psyllabus.platform.student.service.mapper;

import io.psyllabus.platform.student.dto.request.StudentCreateRequest;
import io.psyllabus.platform.student.dto.responses.StudentDetailResponse;
import io.psyllabus.platform.student.dto.responses.StudentResponse;
import io.psyllabus.platform.student.entities.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper for converting Student entities to DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StudentMapper {

    @Mapping(target = "status", source = "currentStatus")
    StudentResponse toResponse(Student student);

    @Mapping(target = "currentClassName", ignore = true)
    StudentDetailResponse toDetailResponse(Student student);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "admissionDate", ignore = true)
    @Mapping(target = "currentStatus", source = "status")
    @Mapping(target = "currentClassId", source = "classId")
    Student toEntity(StudentCreateRequest request);
}
