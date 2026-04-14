package io.psyllabus.platform.academic.service.mapper;

import io.psyllabus.platform.academic.dto.response.TeacherAssignmentResponse;
import io.psyllabus.platform.academic.entities.AcademicTerm;
import io.psyllabus.platform.academic.entities.SchoolClass;
import io.psyllabus.platform.academic.entities.Subject;
import io.psyllabus.platform.academic.entities.TeacherAssignment;
import io.psyllabus.platform.staff.entities.Staff;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper for TeacherAssignment entities.
 * Handles the aggregation of multiple entities into a single response DTO.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TeacherAssignmentMapper {

    @Mapping(target = "id", source = "assignment.id")
    @Mapping(target = "staffId", source = "assignment.staffId")
    @Mapping(target = "userId", source = "staff.userId")
    @Mapping(target = "staffName", expression = "java(staff.getFirstName() + \" \" + staff.getLastName())")
    @Mapping(target = "classId", source = "assignment.classId")
    @Mapping(target = "className", source = "schoolClass.name")
    @Mapping(target = "subjectId", source = "assignment.subjectId")
    @Mapping(target = "subjectName", source = "subject.name")
    @Mapping(target = "academicTermId", source = "assignment.academicTermId")
    @Mapping(target = "academicTermName", source = "term.name")
    @Mapping(target = "isClassTeacher", source = "assignment.isClassTeacher")
    @Mapping(target = "lessonsPerWeek", source = "assignment.lessonsPerWeek")
    @Mapping(target = "status", source = "assignment.status")
    @Mapping(target = "createdAt", source = "assignment.createdAt")
    @Mapping(target = "updatedAt", source = "assignment.updatedAt")
    TeacherAssignmentResponse toResponse(
            TeacherAssignment assignment,
            Staff staff,
            SchoolClass schoolClass,
            Subject subject,
            AcademicTerm term
    );
}
