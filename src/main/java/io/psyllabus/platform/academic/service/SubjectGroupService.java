package io.psyllabus.platform.academic.service;

import io.psyllabus.platform.academic.dto.request.AssignHodRequest;
import io.psyllabus.platform.academic.dto.request.SubjectGroupRequest;
import io.psyllabus.platform.academic.dto.response.SubjectGroupResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for managing {@link io.psyllabus.platform.academic.entities.SubjectGroup}.
 */
public interface SubjectGroupService {
    SubjectGroupResponse create(SubjectGroupRequest request);
    SubjectGroupResponse update(UUID id, SubjectGroupRequest request);
    SubjectGroupResponse getById(UUID id);
    Page<SubjectGroupResponse> getAll(Pageable pageable);
    void delete(UUID id);
    SubjectGroupResponse assignHod(UUID id, AssignHodRequest request);
    void toggleActive(UUID id);
}
