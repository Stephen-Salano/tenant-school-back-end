package io.psyllabus.platform.academic.service.impl;

import io.psyllabus.platform.academic.dto.request.AssignHodRequest;
import io.psyllabus.platform.academic.dto.request.SubjectGroupRequest;
import io.psyllabus.platform.academic.dto.response.SubjectGroupResponse;
import io.psyllabus.platform.academic.entities.SubjectGroup;
import io.psyllabus.platform.academic.repository.SubjectGroupRepository;
import io.psyllabus.platform.academic.service.SubjectGroupService;
import io.psyllabus.platform.academic.service.mapper.SubjectGroupMapper;
import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.common.exception.DuplicateResourceException;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.exception.ValidationException;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of {@link SubjectGroupService}.
 * Handles curriculum-neutral grouping of subjects with tenant-scoping and HOD validation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectGroupServiceImpl implements SubjectGroupService {

    public static final String SUBJECT_GROUP = "SUBJECT_GROUP";
    private final SubjectGroupRepository subjectGroupRepository;
    private final StaffRepository staffRepository;
    private final SubjectGroupMapper subjectGroupMapper;
    private final AuditService auditService;

    @Override
    @Transactional
    public SubjectGroupResponse create(SubjectGroupRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        if (subjectGroupRepository.existsByNameAndTenantId(request.name(), tenantId)) {
            throw new DuplicateResourceException("Subject group already exists: " + request.name());
        }

        SubjectGroup subjectGroup = SubjectGroup.builder()
                .tenantId(tenantId)
                .name(request.name())
                .description(request.description())
                .isActive(true)
                .build();

        SubjectGroup saved = subjectGroupRepository.save(subjectGroup);
        log.info("Subject group created: {} for tenant: {}", saved.getName(), tenantId);

        auditService.log(AuditAction.ACADEMIC_SETUP, SUBJECT_GROUP, saved.getId(), "Created subject group: " + saved.getName());

        return subjectGroupMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SubjectGroupResponse update(UUID id, SubjectGroupRequest request) {
        SubjectGroup subjectGroup = getEntityById(id);
        UUID tenantId = TenantContext.getCurrentTenantId();

        // Check name uniqueness if changed
        if (!subjectGroup.getName().equalsIgnoreCase(request.name()) &&
                subjectGroupRepository.existsByNameAndTenantId(request.name(), tenantId)) {
            throw new DuplicateResourceException("Subject group already exists: " + request.name());
        }

        subjectGroup.setName(request.name());
        subjectGroup.setDescription(request.description());

        SubjectGroup updated = subjectGroupRepository.save(subjectGroup);
        log.info("Subject group updated: {}", id);

        auditService.log(AuditAction.UPDATE, SUBJECT_GROUP, id, "Updated subject group details: " + updated.getName());

        return subjectGroupMapper.toResponse(updated);
    }

    @Override
    public SubjectGroupResponse getById(UUID id) {
        return subjectGroupMapper.toResponse(getEntityById(id));
    }

    @Override
    public Page<SubjectGroupResponse> getAll(Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return subjectGroupRepository.findAllByTenantId(tenantId, pageable)
                .map(subjectGroupMapper::toResponse);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        SubjectGroup subjectGroup = getEntityById(id);
        subjectGroupRepository.delete(subjectGroup);
        log.info("Subject group deleted: {}", id);

        auditService.log(AuditAction.DELETE, SUBJECT_GROUP, id, "Deleted subject group: " + subjectGroup.getName());
    }

    @Override
    @Transactional
    public SubjectGroupResponse assignHod(UUID id, AssignHodRequest request) {
        SubjectGroup subjectGroup = getEntityById(id);
        UUID tenantId = TenantContext.getCurrentTenantId();

        // Validate HOD belongs to the same tenant
        staffRepository.findById(request.staffId())
                .filter(staff -> staff.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ValidationException("Invalid HOD: Staff must belong to the same school."));

        subjectGroup.setHodStaffId(request.staffId());
        SubjectGroup updated = subjectGroupRepository.save(subjectGroup);
        log.info("HOD assigned to subject group: {}", id);

        auditService.log(AuditAction.ACADEMIC_SETUP, SUBJECT_GROUP, id, "Assigned HOD to subject group: " + request.staffId());

        return subjectGroupMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void toggleActive(UUID id) {
        SubjectGroup subjectGroup = getEntityById(id);
        subjectGroup.setActive(!subjectGroup.isActive());
        subjectGroupRepository.save(subjectGroup);
        log.info("Subject group active status toggled: {} (now {})", id, subjectGroup.isActive());

        auditService.log(AuditAction.UPDATE, SUBJECT_GROUP, id, "Toggled active status: " + subjectGroup.isActive());
    }

    private SubjectGroup getEntityById(UUID id) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return subjectGroupRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject group not found: " + id));
    }
}
