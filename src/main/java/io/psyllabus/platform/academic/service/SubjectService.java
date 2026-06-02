package io.psyllabus.platform.academic.service;

import io.psyllabus.platform.academic.dto.request.SubjectCreateRequest;
import io.psyllabus.platform.academic.dto.request.SubjectUpdateRequest;
import io.psyllabus.platform.academic.dto.response.SubjectResponse;
import io.psyllabus.platform.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

/**
 * Manages subjects within the school curriculum.
 */
public interface SubjectService {
    /**
     * Create a new subject.
     * @param request subject details
     * @return the created subject
     */
    SubjectResponse createSubject(SubjectCreateRequest request);

    /**
     * Update subject info (Partial update).
     */
    SubjectResponse updateSubject(UUID id, SubjectUpdateRequest request);

    /**
     * Soft delete subject (sets status to INACTIVE).
     */
    void deleteSubject(UUID id);

    /**
     * List all subjects for the current tenant.
     * @param pageable pagination parameters
     * @return page of subjects
     */
    PageResponse<SubjectResponse> listSubjects(Pageable pageable);

    /**
     * Fetch a subject by ID.
     * @param id the subject UUID
     * @return the subject details
     */
    SubjectResponse getSubjectById(UUID id);
}
