package io.psyllabus.platform.grading.service;

import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.exception.ValidationException;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.grading.dto.request.GradingLevelCreateRequest;
import io.psyllabus.platform.grading.dto.request.GradingLevelUpdateRequest;
import io.psyllabus.platform.grading.dto.response.GradingLevelResponse;
import io.psyllabus.platform.grading.entities.GradingLevel;
import io.psyllabus.platform.grading.repository.GradingLevelRepository;
import io.psyllabus.platform.grading.repository.GradingSchemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GradingLevelServiceImpl implements GradingLevelService {

    public static final String GRADING_LEVEL_NOT_FOUND = "Grading level not found: ";
    private final GradingLevelRepository gradingLevelRepository;
    private final GradingSchemeRepository gradingSchemeRepository;

    @Override
    public GradingLevelResponse createLevel(UUID schemeId, GradingLevelCreateRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        log.info("Create grading level: scheme={}, tenant={}", schemeId, tenantId);

        validateSchemeExists(schemeId, tenantId);
        validatePercentageRange(request.minPercentage(), request.maxPercentage());

        GradingLevel level = GradingLevel.builder()
                .schemeId(schemeId)
                .tenantId(tenantId)
                .label(request.label())
                .minPercentage(request.minPercentage())
                .maxPercentage(request.maxPercentage())
                .sortOrder(request.sortOrder())
                .description(request.description())
                .build();

        level = gradingLevelRepository.save(level);
        log.info("Grading level created: {} for scheme {}", level.getId(), schemeId);
        return toResponse(level);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradingLevelResponse> listByScheme(UUID schemeId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        log.debug("List grading levels for scheme: {} tenant: {}", schemeId, tenantId);

        validateSchemeExists(schemeId, tenantId);

        return gradingLevelRepository.findAllBySchemeIdAndTenantIdOrderBySortOrderAsc(schemeId, tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GradingLevelResponse getById(UUID levelId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        log.debug("Get grading level: {} tenant: {}", levelId, tenantId);

        GradingLevel level = gradingLevelRepository.findById(levelId)
                .filter(l -> l.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException(GRADING_LEVEL_NOT_FOUND + levelId));

        return toResponse(level);
    }

    @Override
    public GradingLevelResponse updateLevel(UUID levelId, GradingLevelUpdateRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        log.info("Update grading level: {} tenant: {}", levelId, tenantId);

        GradingLevel level = gradingLevelRepository.findById(levelId)
                .filter(l -> l.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException(GRADING_LEVEL_NOT_FOUND + levelId));

        validatePercentageRange(request.minPercentage(), request.maxPercentage());

        level.setLabel(request.label());
        level.setMinPercentage(request.minPercentage());
        level.setMaxPercentage(request.maxPercentage());
        level.setSortOrder(request.sortOrder());
        level.setDescription(request.description());

        level = gradingLevelRepository.save(level);
        log.info("Grading level updated: {}", levelId);
        return toResponse(level);
    }

    @Override
    public void deleteLevel(UUID levelId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        log.info("Delete grading level: {} tenant: {}", levelId, tenantId);

        GradingLevel level = gradingLevelRepository.findById(levelId)
                .filter(l -> l.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException(GRADING_LEVEL_NOT_FOUND + levelId));

        gradingLevelRepository.delete(level);
        log.info("Grading level deleted: {}", levelId);
    }

    private void validateSchemeExists(UUID schemeId, UUID tenantId) {
        gradingSchemeRepository.findByIdAndTenantId(schemeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Grading scheme not found: " + schemeId));
    }

    private void validatePercentageRange(BigDecimal min, BigDecimal max) {
        if (min.compareTo(max) > 0) {
            throw new ValidationException("Minimum percentage cannot exceed maximum percentage");
        }
    }

    private GradingLevelResponse toResponse(GradingLevel level) {
        return new GradingLevelResponse(
                level.getId(),
                level.getLabel(),
                level.getMinPercentage(),
                level.getMaxPercentage(),
                level.getSortOrder(),
                level.getDescription()
        );
    }
}