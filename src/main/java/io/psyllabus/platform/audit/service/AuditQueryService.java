package io.psyllabus.platform.audit.service;

import io.psyllabus.platform.audit.dto.request.AuditExportRequest;
import io.psyllabus.platform.audit.dto.request.AuditLogQueryRequest;
import io.psyllabus.platform.audit.dto.response.AuditLogResponse;
import io.psyllabus.platform.audit.entitiy.AuditLog;
import io.psyllabus.platform.audit.repository.AuditLogRepository;
import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.auth.enums.AccessScope;
import io.psyllabus.platform.auth.enums.UserType;
import io.psyllabus.platform.auth.repository.UserRepository;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.common.util.PaginationUtils;
import io.psyllabus.platform.common.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public PageResponse<AuditLogResponse> listLogs(AuditLogQueryRequest query, UUID requestingUserId, UserType userType, AccessScope accessScope) {
        Pageable pageable = PaginationUtils.of(query.page(), query.size(), "createdAt");
        UUID tenantId = (accessScope == AccessScope.PLATFORM && userType == UserType.PLATFORM_ADMIN)
                ? null
                : TenantContext.getCurrentTenantId();

        Page<AuditLog> page = auditLogRepository.findAll(
                buildSpecification(
                        tenantId,
                        query.action(),
                        query.entityType(),
                        query.entityId(),
                        query.userId(),
                        query.fromDate(),
                        query.toDate()
                ),
                pageable
        );
        Map<UUID, User> userMap = loadUsers(page.getContent());

        return PageResponse.from(page.map(l -> toResponse(l, userMap.get(l.getUserId()))));
    }

    public PageResponse<AuditLogResponse> listByUser(UUID userId, int page, int size) {
        Pageable pageable = PaginationUtils.of(page, size, "createdAt");
        Page<AuditLog> pageResult = auditLogRepository.findAll(
                buildSpecification(null, null, null, null, userId, null, null),
                pageable
        );

        Map<UUID, User> userMap = loadUsers(pageResult.getContent());

        return PageResponse.from(pageResult.map(l -> toResponse(l, userMap.get(l.getUserId()))));
    }

    public List<AuditLogResponse> listPlatformLogsForExport(AuditExportRequest request) {
        List<AuditLog> logs = auditLogRepository.findAll(
                buildSpecification(
                        null,
                        request.action(),
                        request.entityType(),
                        request.entityId(),
                        request.userId(),
                        request.fromDate(),
                        request.toDate()
                ),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Map<UUID, User> userMap = loadUsers(logs);
        return logs.stream()
                .map(log -> toResponse(log, userMap.get(log.getUserId())))
                .toList();
    }

    private Map<UUID, User> loadUsers(List<AuditLog> logs) {
        List<UUID> userIds = logs.stream()
                .map(AuditLog::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private AuditLogResponse toResponse(AuditLog log, User user) {
        String userName = (user != null) ? user.getFirstName() + " " + user.getLastName() : null;
        return new AuditLogResponse(
                log.getId(),
                log.getTenantId(),
                log.getUserId(),
                userName,
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails(),
                log.getIpAddress(),
                log.getCreatedAt()
        );
    }

    private Specification<AuditLog> buildSpecification(
            UUID tenantId,
            io.psyllabus.platform.audit.enums.AuditAction action,
            String entityType,
            UUID entityId,
            UUID userId,
            java.time.LocalDateTime fromDate,
            java.time.LocalDateTime toDate
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (tenantId != null) {
                predicates.add(criteriaBuilder.equal(root.get("tenantId"), tenantId));
            }
            if (action != null) {
                predicates.add(criteriaBuilder.equal(root.get("action"), action));
            }
            if (StringUtils.hasText(entityType)) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("entityType")),
                        entityType.trim().toLowerCase()
                ));
            }
            if (entityId != null) {
                predicates.add(criteriaBuilder.equal(root.get("entityId"), entityId));
            }
            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("userId"), userId));
            }
            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
