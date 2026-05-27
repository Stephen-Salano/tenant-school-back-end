package io.psyllabus.platform.grading.service;

import io.psyllabus.platform.academic.dto.events.CurriculumSelectedEvent;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.common.exception.ValidationException;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.grading.dto.request.GradingLevelCreateRequest;
import io.psyllabus.platform.grading.dto.request.GradingSchemeCreateRequest;
import io.psyllabus.platform.grading.dto.response.GradeCalculationResponse;
import io.psyllabus.platform.grading.dto.response.GradingSchemeResponse;
import io.psyllabus.platform.grading.entities.GradingLevel;
import io.psyllabus.platform.grading.entities.GradingScheme;
import io.psyllabus.platform.grading.enums.GradingSystem;
import io.psyllabus.platform.grading.repository.GradingLevelRepository;
import io.psyllabus.platform.grading.repository.GradingSchemeRepository;
import io.psyllabus.platform.school.enums.CurriculumType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradingSchemeServiceImplTest {

    @Mock private GradingSchemeRepository gradingSchemeRepository;
    @Mock private GradingLevelRepository gradingLevelRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private GradingSchemeServiceImpl gradingSchemeService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createScheme_rejectsOverlappingRanges() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);

        GradingSchemeCreateRequest request = new GradingSchemeCreateRequest(
                "Broken Scheme",
                GradingSystem.TRADITIONAL,
                true,
                true,
                List.of(
                        new GradingLevelCreateRequest("A", new BigDecimal("50.00"), new BigDecimal("100.00"), 1, null),
                        new GradingLevelCreateRequest("B", new BigDecimal("40.00"), new BigDecimal("60.00"), 2, null)
                )
        );

        assertThrows(ValidationException.class, () -> gradingSchemeService.createScheme(request));
    }

    @Test
    void resolveGrade_returnsBoundaryLevelForExactPercentage() {
        UUID tenantId = UUID.randomUUID();
        UUID schemeId = UUID.randomUUID();

        GradingScheme scheme = GradingScheme.builder()
                .id(schemeId)
                .tenantId(tenantId)
                .name("Traditional")
                .gradingSystem(GradingSystem.TRADITIONAL)
                .isDefault(true)
                .isActive(true)
                .build();

        when(gradingSchemeRepository.findByTenantIdAndIsDefaultTrue(tenantId)).thenReturn(Optional.of(scheme));
        when(gradingLevelRepository.findAllBySchemeIdAndTenantIdOrderBySortOrderAsc(schemeId, tenantId)).thenReturn(List.of(
                GradingLevel.builder().id(UUID.randomUUID()).schemeId(schemeId).tenantId(tenantId).label("A")
                        .minPercentage(new BigDecimal("80.00")).maxPercentage(new BigDecimal("100.00")).sortOrder(1).build(),
                GradingLevel.builder().id(UUID.randomUUID()).schemeId(schemeId).tenantId(tenantId).label("B")
                        .minPercentage(new BigDecimal("70.00")).maxPercentage(new BigDecimal("79.99")).sortOrder(2).build()
        ));

        GradeCalculationResponse response = gradingSchemeService.resolveGrade(tenantId, 80, 100);

        assertEquals("A", response.label());
        assertEquals(new BigDecimal("80.00"), response.percentage());
    }

    @Test
    void onCurriculumSelected_reusesExistingSeededScheme() {
        UUID tenantId = UUID.randomUUID();
        UUID schemeId = UUID.randomUUID();
        GradingScheme existing = GradingScheme.builder()
                .id(schemeId)
                .tenantId(tenantId)
                .name("Traditional A-F Default")
                .gradingSystem(GradingSystem.TRADITIONAL)
                .isDefault(false)
                .isActive(false)
                .build();

        when(gradingSchemeRepository.findByTenantIdAndNameIgnoreCase(tenantId, "Traditional A-F Default"))
                .thenReturn(Optional.of(existing));
        when(gradingSchemeRepository.findByTenantIdAndIsDefaultTrue(tenantId)).thenReturn(Optional.of(existing));

        gradingSchemeService.onCurriculumSelected(new CurriculumSelectedEvent(tenantId, CurriculumType.IGCSE, 2026));

        ArgumentCaptor<GradingScheme> captor = ArgumentCaptor.forClass(GradingScheme.class);
        verify(gradingSchemeRepository).save(captor.capture());
        assertEquals(schemeId, captor.getValue().getId());
        assertTrue(captor.getValue().isDefault());
        assertTrue(captor.getValue().isActive());
        verify(gradingLevelRepository).deleteAllBySchemeIdAndTenantId(schemeId, tenantId);
        verify(gradingLevelRepository).saveAll(anyList());
    }

    @Test
    void listSchemes_usesBatchLevelCountQuery() {
        UUID tenantId = UUID.randomUUID();
        UUID schemeId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);

        GradingScheme scheme = GradingScheme.builder()
                .id(schemeId)
                .tenantId(tenantId)
                .name("Traditional A-F")
                .gradingSystem(GradingSystem.TRADITIONAL)
                .isDefault(true)
                .isActive(true)
                .build();
        Page<GradingScheme> page = new PageImpl<>(List.of(scheme), PageRequest.of(0, 20), 1);

        when(gradingSchemeRepository.findAllByTenantId(tenantId, PageRequest.of(0, 20))).thenReturn(page);
        List<Object[]> levelCounts = new ArrayList<>();
        levelCounts.add(new Object[]{schemeId, 5L});
        when(gradingLevelRepository.countBySchemeIds(List.of(schemeId), tenantId))
                .thenReturn(levelCounts);

        PageResponse<GradingSchemeResponse> response = gradingSchemeService.listSchemes(PageRequest.of(0, 20));

        assertEquals(1, response.content().size());
        assertEquals(Integer.valueOf(5), response.content().getFirst().levelCount());
        verify(gradingLevelRepository).countBySchemeIds(List.of(schemeId), tenantId);
    }
}
