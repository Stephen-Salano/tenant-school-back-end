package io.psyllabus.platform.academic.service.impl;

import io.psyllabus.platform.academic.dto.request.SubjectCategoryCreateRequest;
import io.psyllabus.platform.academic.dto.response.SubjectCategoryResponse;
import io.psyllabus.platform.academic.entities.SubjectCategory;
import io.psyllabus.platform.academic.repository.SubjectCategoryRepository;
import io.psyllabus.platform.academic.service.SubjectCategoryService;
import io.psyllabus.platform.common.exception.DuplicateResourceException;
import io.psyllabus.platform.common.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link SubjectCategoryService}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SubjectCategoryServiceImpl implements SubjectCategoryService {

    private final SubjectCategoryRepository subjectCategoryRepository;

    @Override
    public SubjectCategoryResponse createCategory(SubjectCategoryCreateRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        if (subjectCategoryRepository.existsByNameAndTenantId(request.name(), tenantId)) {
            throw new DuplicateResourceException("Subject category '" + request.name() + "' already exists");
        }

        SubjectCategory category = SubjectCategory.builder()
                .tenantId(tenantId)
                .name(request.name())
                .description(request.description())
                .isActive(true)
                .build();

        SubjectCategory saved = subjectCategoryRepository.save(category);
        log.info("Subject category created: id={} name={} tenant={}", saved.getId(), saved.getName(), tenantId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectCategoryResponse> listCategories() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return subjectCategoryRepository.findAllByTenantId(tenantId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private SubjectCategoryResponse mapToResponse(SubjectCategory category) {
        return new SubjectCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
