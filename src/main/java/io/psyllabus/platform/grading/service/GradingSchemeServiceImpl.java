package io.psyllabus.platform.grading.service;

import io.psyllabus.platform.academic.dto.events.CurriculumSelectedEvent;
import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.common.exception.DuplicateResourceException;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.exception.ValidationException;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.grading.dto.request.GradingLevelCreateRequest;
import io.psyllabus.platform.grading.dto.request.GradingSchemeCreateRequest;
import io.psyllabus.platform.grading.dto.request.GradingSchemeStatusUpdateRequest;
import io.psyllabus.platform.grading.dto.request.GradingSchemeUpdateRequest;
import io.psyllabus.platform.grading.dto.response.GradeCalculationResponse;
import io.psyllabus.platform.grading.dto.response.GradingLevelResponse;
import io.psyllabus.platform.grading.dto.response.GradingSchemeDetailResponse;
import io.psyllabus.platform.grading.dto.response.GradingSchemeResponse;
import io.psyllabus.platform.grading.entities.GradingLevel;
import io.psyllabus.platform.grading.entities.GradingScheme;
import io.psyllabus.platform.grading.enums.GradingSystem;
import io.psyllabus.platform.grading.repository.GradingLevelRepository;
import io.psyllabus.platform.grading.repository.GradingSchemeRepository;
import io.psyllabus.platform.school.enums.CurriculumType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GradingSchemeServiceImpl implements GradingSchemeService {

    private static final String ENTITY_TYPE = "GRADING_SCHEME";
    public static final String TOTAL = "100.00";
    private static final BigDecimal HUNDRED = new BigDecimal(TOTAL);
    private static final String CBC_DEFAULT_NAME = "CBC Default";
    private static final String TRADITIONAL_DEFAULT_NAME = "Traditional A-F Default";

    private final GradingSchemeRepository gradingSchemeRepository;
    private final GradingLevelRepository gradingLevelRepository;
    private final AuditService auditService;

    @Override
    public GradingSchemeDetailResponse createScheme(GradingSchemeCreateRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        validateDuplicateName(tenantId, request.name(), null);
        validateLevels(request.levels());

        GradingScheme scheme = gradingSchemeRepository.save(GradingScheme.builder()
                .tenantId(tenantId)
                .name(request.name().trim())
                .gradingSystem(request.gradingSystem())
                .isDefault(Boolean.TRUE.equals(request.isDefault()))
                .isActive(request.isActive() == null || request.isActive())
                .build());

        if (scheme.isDefault()) {
            scheme.setActive(true);
            clearOtherDefaults(tenantId, scheme.getId());
        }

        saveLevels(scheme, request.levels());
        auditService.log(AuditAction.CREATE, ENTITY_TYPE, scheme.getId(), "Created grading scheme " + scheme.getName());
        return toDetailResponse(scheme, loadLevels(scheme.getId(), tenantId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<GradingSchemeResponse> listSchemes(Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return PageResponse.from(gradingSchemeRepository.findAllByTenantId(tenantId, pageable)
                .map(this::toSummaryResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public GradingSchemeDetailResponse getScheme(UUID schemeId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        GradingScheme scheme = findScheme(schemeId, tenantId);
        return toDetailResponse(scheme, loadLevels(schemeId, tenantId));
    }

    @Override
    public GradingSchemeDetailResponse updateScheme(UUID schemeId, GradingSchemeUpdateRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        GradingScheme scheme = findScheme(schemeId, tenantId);

        if (request.name() != null && !request.name().isBlank()) {
            validateDuplicateName(tenantId, request.name(), schemeId);
            scheme.setName(request.name().trim());
        }
        if (request.gradingSystem() != null) {
            scheme.setGradingSystem(request.gradingSystem());
        }
        if (request.isActive() != null) {
            if (!request.isActive() && scheme.isDefault()) {
                throw new ValidationException("Default grading scheme cannot be deactivated");
            }
            scheme.setActive(request.isActive());
        }
        if (Boolean.TRUE.equals(request.isDefault())) {
            scheme.setDefault(true);
            scheme.setActive(true);
            clearOtherDefaults(tenantId, scheme.getId());
        }

        gradingSchemeRepository.save(scheme);

        if (request.levels() != null) {
            validateLevels(request.levels());
            gradingLevelRepository.deleteAllBySchemeIdAndTenantId(schemeId, tenantId);
            saveLevels(scheme, request.levels());
        }

        auditService.log(AuditAction.UPDATE, ENTITY_TYPE, scheme.getId(), "Updated grading scheme " + scheme.getName());
        return toDetailResponse(scheme, loadLevels(schemeId, tenantId));
    }

    @Override
    public GradingSchemeDetailResponse updateStatus(UUID schemeId, GradingSchemeStatusUpdateRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        GradingScheme scheme = findScheme(schemeId, tenantId);
        if (!request.active() && scheme.isDefault()) {
            throw new ValidationException("Default grading scheme cannot be deactivated");
        }

        scheme.setActive(request.active());
        gradingSchemeRepository.save(scheme);
        auditService.log(AuditAction.UPDATE, ENTITY_TYPE, scheme.getId(),
                (request.active() ? "Activated " : "Deactivated ") + "grading scheme " + scheme.getName());
        return toDetailResponse(scheme, loadLevels(schemeId, tenantId));
    }

    @Override
    public GradingSchemeDetailResponse setDefaultScheme(UUID schemeId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        GradingScheme scheme = findScheme(schemeId, tenantId);
        scheme.setDefault(true);
        scheme.setActive(true);
        gradingSchemeRepository.save(scheme);
        clearOtherDefaults(tenantId, schemeId);
        auditService.log(AuditAction.UPDATE, ENTITY_TYPE, scheme.getId(), "Set grading scheme as default: " + scheme.getName());
        return toDetailResponse(scheme, loadLevels(schemeId, tenantId));
    }

    @Override
    @Transactional(readOnly = true)
    public GradeCalculationResponse resolveGrade(UUID tenantId, int score, int maxScore) {
        if (maxScore <= 0) {
            throw new ValidationException("Max score must be greater than zero");
        }
        if (score < 0 || score > maxScore) {
            throw new ValidationException("Score must be between 0 and " + maxScore);
        }

        GradingScheme scheme = gradingSchemeRepository.findByTenantIdAndIsDefaultTrue(tenantId)
                .filter(GradingScheme::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("No active default grading scheme found"));

        BigDecimal percentage = BigDecimal.valueOf(score)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(maxScore), 2, RoundingMode.HALF_UP);

        GradingLevel level = loadLevels(scheme.getId(), tenantId).stream()
                .filter(candidate -> candidate.getMinPercentage().compareTo(percentage) <= 0
                        && candidate.getMaxPercentage().compareTo(percentage) >= 0)
                .findFirst()
                .orElseThrow(() -> new ValidationException("No grading level matches percentage " + percentage));

        return new GradeCalculationResponse(
                scheme.getId(),
                scheme.getName(),
                scheme.getGradingSystem(),
                level.getLabel(),
                percentage
        );
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCurriculumSelected(CurriculumSelectedEvent event) {
        UUID previousTenant = TenantContext.getCurrentTenantId();
        TenantContext.setCurrentTenant(event.tenantId());
        try {
            seedDefaultScheme(event.tenantId(), event.curriculumType());
        } finally {
            if (previousTenant == null) {
                TenantContext.clear();
            } else {
                TenantContext.setCurrentTenant(previousTenant);
            }
        }
    }

    private void seedDefaultScheme(UUID tenantId, CurriculumType curriculumType) {
        if (curriculumType == CurriculumType.CBC) {
            upsertSeededDefault(
                    tenantId,
                    CBC_DEFAULT_NAME,
                    GradingSystem.CBC,
                    List.of(
                            new GradingLevelCreateRequest("Exceeds Expectation", new BigDecimal("75.00"), new BigDecimal(TOTAL), 1, null),
                            new GradingLevelCreateRequest("Meets Expectation", new BigDecimal("50.00"), new BigDecimal("74.99"), 2, null),
                            new GradingLevelCreateRequest("Approaching Expectation", new BigDecimal("25.00"), new BigDecimal("49.99"), 3, null),
                            new GradingLevelCreateRequest("Below Expectation", new BigDecimal("0.00"), new BigDecimal("24.99"), 4, null)
                    )
            );
            return;
        }

        upsertSeededDefault(
                tenantId,
                TRADITIONAL_DEFAULT_NAME,
                GradingSystem.TRADITIONAL,
                List.of(
                        new GradingLevelCreateRequest("A", new BigDecimal("80.00"), new BigDecimal(TOTAL), 1, null),
                        new GradingLevelCreateRequest("B", new BigDecimal("70.00"), new BigDecimal("79.99"), 2, null),
                        new GradingLevelCreateRequest("C", new BigDecimal("60.00"), new BigDecimal("69.99"), 3, null),
                        new GradingLevelCreateRequest("D", new BigDecimal("50.00"), new BigDecimal("59.99"), 4, null),
                        new GradingLevelCreateRequest("E", new BigDecimal("40.00"), new BigDecimal("49.99"), 5, null),
                        new GradingLevelCreateRequest("F", new BigDecimal("0.00"), new BigDecimal("39.99"), 6, null)
                )
        );
    }

    private void upsertSeededDefault(UUID tenantId, String name, GradingSystem system, List<GradingLevelCreateRequest> levels) {
        validateLevels(levels);

        GradingScheme scheme = gradingSchemeRepository.findByTenantIdAndNameIgnoreCase(tenantId, name)
                .orElseGet(() -> gradingSchemeRepository.save(GradingScheme.builder()
                        .tenantId(tenantId)
                        .name(name)
                        .gradingSystem(system)
                        .isDefault(true)
                        .isActive(true)
                        .build()));

        scheme.setGradingSystem(system);
        scheme.setDefault(true);
        scheme.setActive(true);
        gradingSchemeRepository.save(scheme);
        clearOtherDefaults(tenantId, scheme.getId());
        gradingLevelRepository.deleteAllBySchemeIdAndTenantId(scheme.getId(), tenantId);
        saveLevels(scheme, levels);
        auditService.log(AuditAction.UPDATE, ENTITY_TYPE, scheme.getId(), "Seeded default grading scheme for curriculum " + system.name());
    }

    private void validateDuplicateName(UUID tenantId, String requestedName, UUID currentSchemeId) {
        gradingSchemeRepository.findByTenantIdAndNameIgnoreCase(tenantId, requestedName.trim())
                .filter(existing -> !existing.getId().equals(currentSchemeId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Grading scheme name already exists");
                });
    }

    private GradingScheme findScheme(UUID schemeId, UUID tenantId) {
        return gradingSchemeRepository.findByIdAndTenantId(schemeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Grading scheme not found: " + schemeId));
    }

    private List<GradingLevel> loadLevels(UUID schemeId, UUID tenantId) {
        return gradingLevelRepository.findAllBySchemeIdAndTenantIdOrderBySortOrderAsc(schemeId, tenantId);
    }

    private void saveLevels(GradingScheme scheme, List<GradingLevelCreateRequest> levelRequests) {
        List<GradingLevel> levels = levelRequests.stream()
                .map(level -> GradingLevel.builder()
                        .schemeId(scheme.getId())
                        .tenantId(scheme.getTenantId())
                        .label(level.label().trim())
                        .minPercentage(scale(level.minPercentage()))
                        .maxPercentage(scale(level.maxPercentage()))
                        .sortOrder(level.sortOrder())
                        .description(level.description())
                        .build())
                .toList();
        gradingLevelRepository.saveAll(levels);
    }

    private void clearOtherDefaults(UUID tenantId, UUID keepSchemeId) {
        gradingSchemeRepository.findByTenantIdAndIsDefaultTrue(tenantId)
                .filter(existing -> !existing.getId().equals(keepSchemeId))
                .ifPresent(existing -> {
                    existing.setDefault(false);
                    gradingSchemeRepository.save(existing);
                });
    }

    private void validateLevels(List<GradingLevelCreateRequest> levelRequests) {
        if (levelRequests == null || levelRequests.isEmpty()) {
            throw new ValidationException("At least one grading level is required");
        }

        Set<Integer> sortOrders = new HashSet<>();
        List<GradingLevelCreateRequest> byRange = new ArrayList<>(levelRequests);
        byRange.sort(Comparator.comparing(GradingLevelCreateRequest::minPercentage));

        BigDecimal previousMax = null;
        for (GradingLevelCreateRequest level : levelRequests) {
            if (!sortOrders.add(level.sortOrder())) {
                throw new ValidationException("Grading level sort order must be unique");
            }
            if (level.minPercentage().compareTo(level.maxPercentage()) > 0) {
                throw new ValidationException("Minimum percentage cannot exceed maximum percentage");
            }
        }

        for (GradingLevelCreateRequest level : byRange) {
            if (previousMax != null && level.minPercentage().compareTo(previousMax) <= 0) {
                throw new ValidationException("Grading level ranges must not overlap");
            }
            previousMax = level.maxPercentage();
        }
    }

    private GradingSchemeResponse toSummaryResponse(GradingScheme scheme) {
        int levelCount = loadLevels(scheme.getId(), scheme.getTenantId()).size();
        return new GradingSchemeResponse(
                scheme.getId(),
                scheme.getName(),
                scheme.getGradingSystem(),
                scheme.isDefault(),
                scheme.isActive(),
                levelCount,
                scheme.getCreatedAt(),
                scheme.getUpdatedAt()
        );
    }

    private GradingSchemeDetailResponse toDetailResponse(GradingScheme scheme, List<GradingLevel> levels) {
        return new GradingSchemeDetailResponse(
                scheme.getId(),
                scheme.getName(),
                scheme.getGradingSystem(),
                scheme.isDefault(),
                scheme.isActive(),
                levels.stream().map(this::toLevelResponse).toList(),
                scheme.getCreatedAt(),
                scheme.getUpdatedAt()
        );
    }

    private GradingLevelResponse toLevelResponse(GradingLevel level) {
        return new GradingLevelResponse(
                level.getId(),
                level.getLabel(),
                level.getMinPercentage(),
                level.getMaxPercentage(),
                level.getSortOrder(),
                level.getDescription()
        );
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
