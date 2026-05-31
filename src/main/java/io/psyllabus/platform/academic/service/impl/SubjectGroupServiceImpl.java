package io.psyllabus.platform.academic.service.impl;

import io.psyllabus.platform.academic.dto.request.AssignHodRequest;
import io.psyllabus.platform.academic.dto.request.SubjectGroupRequest;
import io.psyllabus.platform.academic.dto.response.SubjectGroupResponse;
import io.psyllabus.platform.academic.entities.SubjectGroup;
import io.psyllabus.platform.academic.repository.SubjectGroupRepository;
import io.psyllabus.platform.academic.service.SubjectGroupService;
import io.psyllabus.platform.common.exception.DuplicateResourceException;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.exception.ValidationException;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.staff.repository.StaffRepository;
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
 * Implementation of {@link SubjectGroupService}.
 * Handles curriculum-neutral grouping of subjects with tenant-scoping and HOD validation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectGroupServiceImpl implements SubjectGroupService {

    private final SubjectGroupRepository subjectGroupRepository;
    private final StaffRepository staffRepository;

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
        return mapToResponse(saved);
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
        return mapToResponse(updated);
    }

    @Override
    public SubjectGroupResponse getById(UUID id) {
        return mapToResponse(getEntityById(id));
    }

    @Override
    public Page<SubjectGroupResponse> getAll(Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Page<SubjectGroup> subjectGroups = subjectGroupRepository.findAllByTenantId(tenantId, pageable);
        Map<UUID, String> hodNames = loadHodNames(subjectGroups, tenantId);
        return new PageImpl<>(
                subjectGroups.getContent().stream()
                        .map(subjectGroup -> mapToResponse(subjectGroup, hodNames))
                        .toList(),
                subjectGroups.getPageable(),
                subjectGroups.getTotalElements()
        );
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        SubjectGroup subjectGroup = getEntityById(id);
        subjectGroupRepository.delete(subjectGroup);
        log.info("Subject group deleted: {}", id);
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
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void toggleActive(UUID id) {
        SubjectGroup subjectGroup = getEntityById(id);
        subjectGroup.setActive(!subjectGroup.isActive());
        subjectGroupRepository.save(subjectGroup);
        log.info("Subject group active status toggled: {} (now {})", id, subjectGroup.isActive());
    }

    private SubjectGroup getEntityById(UUID id) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return subjectGroupRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject group not found: " + id));
    }

    private SubjectGroupResponse mapToResponse(SubjectGroup entity) {
        Map<UUID, String> hodNames = entity.getHodStaffId() == null
                ? Map.of()
                : Map.of(entity.getHodStaffId(),
                    staffRepository.findDisplayNameByIdAndTenantId(entity.getHodStaffId(), entity.getTenantId()).orElse(""));
        return mapToResponse(entity, hodNames);
    }

    private SubjectGroupResponse mapToResponse(SubjectGroup entity, Map<UUID, String> hodNames) {
        String hodStaffName = entity.getHodStaffId() == null
                ? null
                : blankToNull(hodNames.get(entity.getHodStaffId()));
        return new SubjectGroupResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getDescription(),
                entity.getHodStaffId(),
                hodStaffName,
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private Map<UUID, String> loadHodNames(Page<SubjectGroup> subjectGroups, UUID tenantId) {
        Set<UUID> ids = subjectGroups.getContent().stream()
                .map(SubjectGroup::getHodStaffId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return staffRepository.findAllByIdInAndTenantId(ids, tenantId).stream()
                .collect(Collectors.toMap(
                        io.psyllabus.platform.staff.entities.Staff::getId,
                        staff -> staff.getFirstName() + " " + staff.getLastName()
                ));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
