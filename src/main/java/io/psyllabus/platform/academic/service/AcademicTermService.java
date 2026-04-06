package io.psyllabus.platform.academic.service;

import io.psyllabus.platform.academic.dto.request.AcademicTermCreateRequest;
import io.psyllabus.platform.academic.dto.request.AcademicTermUpdateRequest;
import io.psyllabus.platform.academic.dto.response.AcademicTermDetailResponse;
import io.psyllabus.platform.academic.dto.response.AcademicTermResponse;
import io.psyllabus.platform.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing academic terms.
 */
public interface AcademicTermService {

    AcademicTermResponse create(AcademicTermCreateRequest request);

    AcademicTermDetailResponse getById(UUID id);

    AcademicTermResponse getCurrentTerm();

    List<AcademicTermResponse> listByYear(UUID academicYearId);

    PageResponse<AcademicTermResponse> listAll(Pageable pageable);

    AcademicTermResponse update(UUID id, AcademicTermUpdateRequest request);

    AcademicTermResponse setCurrentTerm(UUID id);
}
