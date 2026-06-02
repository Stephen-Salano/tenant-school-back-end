package io.psyllabus.platform.academic.service;

import io.psyllabus.platform.academic.dto.request.ClassCreateRequest;
import io.psyllabus.platform.academic.dto.request.ClassUpdateRequest;
import io.psyllabus.platform.academic.dto.response.ClassDetailResponse;
import io.psyllabus.platform.academic.dto.response.ClassEnrollmentStatsResponse;
import io.psyllabus.platform.academic.dto.response.ClassResponse;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.student.dto.responses.StudentResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Manages school classes (sections) within academic years.
 *
 * <p>Stream names are validated against {@code SchoolProfile.streamNames} on creation
 * to prevent arbitrary free-text that breaks later filtering and reporting.</p>
 */
public interface ClassService {

    /**
     * Create a new class within an academic year.
     *
     * @param request class details; {@code streamName} validated against school profile
     * @return the created class
     * @throws io.psyllabus.platform.common.exception.DuplicateResourceException if class name exists for that year
     * @throws io.psyllabus.platform.common.exception.ValidationException if stream name is not in school's configured list
     * @throws io.psyllabus.platform.common.exception.ResourceNotFoundException if academic year not found
     */
    ClassResponse createClass(ClassCreateRequest request);

    /**
     * Assign a class (homeroom) teacher.
     *
     * <p>The staff member must already have an active {@link io.psyllabus.platform.academic.entities.TeacherAssignment}
     * in this class for any subject. The method demotes any existing class teacher atomically
     * before promoting the new one.</p>
     *
     * @param classId the class UUID
     * @param staffId the staff member to promote
     * @throws io.psyllabus.platform.common.exception.ResourceNotFoundException if class or active assignment not found
     */
    void assignClassTeacher(UUID classId, UUID staffId);

    /**
     * Paginated list of classes for a specific academic year, tenant-scoped.
     *
     * @param yearId the academic year UUID
     * @param pageable pagination parameters
     * @return page of classes
     */
    PageResponse<ClassResponse> getClassesByYear(UUID yearId, Pageable pageable);

    /**
     * Fetch full class detail by ID, tenant-scoped.
     * Includes computed enrollment count and available slots.
     *
     * @param classId the class UUID
     * @return class detail including enrollment stats
     * @throws io.psyllabus.platform.common.exception.ResourceNotFoundException if not found
     */
    ClassDetailResponse getClassById(UUID classId);

    /**
     * Enrollment statistics for a single class.
     *
     * @param classId the class UUID
     * @return stats including enrolled count, max capacity, and available slots
     */
    ClassEnrollmentStatsResponse getClassEnrollmentStats(UUID classId);

    /**
     * Return the configured stream names from the school's profile.
     * Used by the frontend to populate the stream name dropdown on class creation.
     *
     * @return list of configured stream names, or empty list if none configured
     */
    List<String> getStreamNames();

    /**
     * Update class info (Partial update).
     */
    ClassResponse updateClass(UUID classId, ClassUpdateRequest request);

    /**
     * Soft delete class (sets status to INACTIVE).
     */
    void deleteClass(UUID classId);

    /**
     * Get roster of active students for a class.
     */
    List<StudentResponse> getClassRoster(UUID classId);
}
