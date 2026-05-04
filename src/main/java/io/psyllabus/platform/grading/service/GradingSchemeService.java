package io.psyllabus.platform.grading.service;

import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.grading.dto.request.GradingSchemeCreateRequest;
import io.psyllabus.platform.grading.dto.request.GradingSchemeStatusUpdateRequest;
import io.psyllabus.platform.grading.dto.request.GradingSchemeUpdateRequest;
import io.psyllabus.platform.grading.dto.response.GradeCalculationResponse;
import io.psyllabus.platform.grading.dto.response.GradingSchemeDetailResponse;
import io.psyllabus.platform.grading.dto.response.GradingSchemeResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface GradingSchemeService {

    GradingSchemeDetailResponse createScheme(GradingSchemeCreateRequest request);

    PageResponse<GradingSchemeResponse> listSchemes(Pageable pageable);

    GradingSchemeDetailResponse getScheme(UUID schemeId);

    GradingSchemeDetailResponse updateScheme(UUID schemeId, GradingSchemeUpdateRequest request);

    GradingSchemeDetailResponse updateStatus(UUID schemeId, GradingSchemeStatusUpdateRequest request);

    GradingSchemeDetailResponse setDefaultScheme(UUID schemeId);

    GradeCalculationResponse resolveGrade(UUID tenantId, int score, int maxScore);
}
