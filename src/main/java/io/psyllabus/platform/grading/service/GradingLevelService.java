package io.psyllabus.platform.grading.service;

import io.psyllabus.platform.grading.dto.request.GradingLevelCreateRequest;
import io.psyllabus.platform.grading.dto.request.GradingLevelUpdateRequest;
import io.psyllabus.platform.grading.dto.response.GradingLevelResponse;

import java.util.List;
import java.util.UUID;

public interface GradingLevelService {

    GradingLevelResponse createLevel(UUID schemeId, GradingLevelCreateRequest request);

    List<GradingLevelResponse> listByScheme(UUID schemeId);

    GradingLevelResponse getById(UUID levelId);

    GradingLevelResponse updateLevel(UUID levelId, GradingLevelUpdateRequest request);

    void deleteLevel(UUID levelId);
}