package io.psyllabus.platform.academic.service.impl;

import io.psyllabus.platform.academic.dto.request.SubjectCreateRequest;
import io.psyllabus.platform.academic.dto.response.SubjectResponse;
import io.psyllabus.platform.academic.entities.Subject;
import io.psyllabus.platform.academic.enums.AcademicStatus;
import io.psyllabus.platform.academic.repository.SubjectCategoryRepository;
import io.psyllabus.platform.academic.repository.SubjectGroupRepository;
import io.psyllabus.platform.academic.repository.SubjectRepository;
import io.psyllabus.platform.academic.service.SubjectService;
import io.psyllabus.platform.academic.service.mapper.SubjectMapper;
import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.common.exception.DuplicateResourceException;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
    private final SubjectMapper subjectMapper;
    private final AuditService auditService;

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
                .status(AcademicStatus.ACTIVE)
                .build();

        Subject saved = subjectRepository.save(subject);
        log.info("Subject created: id={} name={} code={} tenant={}", saved.getId(), saved.getName(), saved.getCode(), tenantId);

        auditService.log(AuditAction.ACADEMIC_SETUP, "SUBJECT", saved.getId(), "Created subject: " + saved.getName());

        return subjectMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubjectResponse> listSubjects(Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return PageResponse.from(
                subjectRepository.findAllByTenantId(tenantId, pageable)
                        .map(subjectMapper::toResponse)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectResponse getSubjectById(UUID id) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Subject subject = subjectRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + id));
        return subjectMapper.toResponse(subject);
    }
}
