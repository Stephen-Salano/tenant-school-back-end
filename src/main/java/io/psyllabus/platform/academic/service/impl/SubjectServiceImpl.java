package io.psyllabus.platform.academic.service.impl;

import io.psyllabus.platform.academic.dto.request.SubjectCreateRequest;
import io.psyllabus.platform.academic.dto.response.SubjectResponse;
import io.psyllabus.platform.academic.entities.Subject;
import io.psyllabus.platform.academic.repository.SubjectCategoryRepository;
import io.psyllabus.platform.academic.repository.SubjectGroupRepository;
import io.psyllabus.platform.academic.repository.SubjectRepository;
import io.psyllabus.platform.academic.service.SubjectService;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.common.exception.DuplicateResourceException;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link SubjectService}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final SubjectGroupRepository subjectGroupRepository;
    private final SubjectCategoryRepository subjectCategoryRepository;

    @Override
    public SubjectResponse createSubject(SubjectCreateRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        // Validate subject group if provided
        if (request.subjectGroupId() != null) {
            subjectGroupRepository.findByIdAndTenantId(request.subjectGroupId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Subject group not found: " + request.subjectGroupId()));
        }

        // Validate subject category if provided
        if (request.subjectCategoryId() != null) {
            subjectCategoryRepository.findByIdAndTenantId(request.subjectCategoryId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Subject category not found: " + request.subjectCategoryId()));
        }

        // Check for duplicate code
        if (request.code() != null && !request.code().isBlank() &&
            subjectRepository.existsByCodeAndTenantId(request.code(), tenantId)) {
            throw new DuplicateResourceException("Subject with code '" + request.code() + "' already exists");
        }

        Subject subject = Subject.builder()
                .tenantId(tenantId)
                .name(request.name())
                .code(request.code())
                .subjectGroupId(request.subjectGroupId())
                .subjectCategoryId(request.subjectCategoryId())
                .maxScore(request.maxScore())
                .passMark(request.passMark())
                .isActive(true)
                .build();

        Subject saved = subjectRepository.save(subject);
        log.info("Subject created: id={} name={} code={} tenant={}", saved.getId(), saved.getName(), saved.getCode(), tenantId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubjectResponse> listSubjects(Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Page<Subject> subjects = subjectRepository.findAllByTenantId(tenantId, pageable);
        Map<UUID, String> subjectGroupNames = loadSubjectGroupNames(subjects, tenantId);
        Map<UUID, String> subjectCategoryNames = loadSubjectCategoryNames(subjects, tenantId);
        Page<SubjectResponse> responsePage = new PageImpl<>(
                subjects.getContent().stream()
                        .map(subject -> mapToResponse(subject, subjectGroupNames, subjectCategoryNames))
                        .toList(),
                subjects.getPageable(),
                subjects.getTotalElements()
        );
        return PageResponse.from(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectResponse getSubjectById(UUID id) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Subject subject = subjectRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + id));
        return mapToResponse(subject);
    }

    private SubjectResponse mapToResponse(Subject subject) {
        UUID tenantId = subject.getTenantId();
        Map<UUID, String> subjectGroupNames = subject.getSubjectGroupId() == null
                ? Map.of()
                : Map.of(subject.getSubjectGroupId(),
                    subjectGroupRepository.findNameByIdAndTenantId(subject.getSubjectGroupId(), tenantId).orElse(""));
        Map<UUID, String> subjectCategoryNames = subject.getSubjectCategoryId() == null
                ? Map.of()
                : Map.of(subject.getSubjectCategoryId(),
                    subjectCategoryRepository.findNameByIdAndTenantId(subject.getSubjectCategoryId(), tenantId).orElse(""));
        return mapToResponse(subject, subjectGroupNames, subjectCategoryNames);
    }

    private SubjectResponse mapToResponse(Subject subject, Map<UUID, String> subjectGroupNames, Map<UUID, String> subjectCategoryNames) {
        String subjectGroupName = subject.getSubjectGroupId() == null
                ? null
                : blankToNull(subjectGroupNames.get(subject.getSubjectGroupId()));
        String subjectCategoryName = subject.getSubjectCategoryId() == null
                ? null
                : blankToNull(subjectCategoryNames.get(subject.getSubjectCategoryId()));
        return new SubjectResponse(
                subject.getId(),
                subject.getName(),
                subject.getCode(),
                subject.getSubjectGroupId(),
                subjectGroupName,
                subject.getSubjectCategoryId(),
                subjectCategoryName,
                subject.getMaxScore(),
                subject.getPassMark(),
                subject.isActive(),
                subject.getCreatedAt(),
                subject.getUpdatedAt()
        );
    }

    private Map<UUID, String> loadSubjectGroupNames(Page<Subject> subjects, UUID tenantId) {
        Set<UUID> ids = subjects.getContent().stream()
                .map(Subject::getSubjectGroupId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return subjectGroupRepository.findAllByIdInAndTenantId(ids, tenantId).stream()
                .collect(Collectors.toMap(
                        io.psyllabus.platform.academic.entities.SubjectGroup::getId,
                        io.psyllabus.platform.academic.entities.SubjectGroup::getName
                ));
    }

    private Map<UUID, String> loadSubjectCategoryNames(Page<Subject> subjects, UUID tenantId) {
        Set<UUID> ids = subjects.getContent().stream()
                .map(Subject::getSubjectCategoryId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return subjectCategoryRepository.findAllByIdInAndTenantId(ids, tenantId).stream()
                .collect(Collectors.toMap(
                        io.psyllabus.platform.academic.entities.SubjectCategory::getId,
                        io.psyllabus.platform.academic.entities.SubjectCategory::getName
                ));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
