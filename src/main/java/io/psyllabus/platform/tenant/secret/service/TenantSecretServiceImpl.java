package io.psyllabus.platform.tenant.secret.service;

import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.tenant.secret.dto.request.CreateSecretRequest;
import io.psyllabus.platform.tenant.secret.dto.request.UpdateSecretRequest;
import io.psyllabus.platform.tenant.secret.dto.response.SecretResponse;
import io.psyllabus.platform.tenant.secret.entity.TenantSecret;
import io.psyllabus.platform.tenant.secret.enums.SecretType;
import io.psyllabus.platform.tenant.secret.repository.TenantSecretRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TenantSecretServiceImpl implements TenantSecretService {

    public static final String TENANT_SECRET = "TenantSecret";
    public static final String SECRET_NOT_FOUND = "Secret not found: ";
    private final TenantSecretRepository tenantSecretRepository;
    private final AuditService auditService;

    @Override
    public SecretResponse create(UUID tenantId, CreateSecretRequest request) {
        TenantSecret existing = tenantSecretRepository
                .findByTenantIdAndSecretType(tenantId, request.secretType())
                .orElse(null);

        if (existing != null) {
            existing.setSecretValue(request.secretValue());
            existing.setDescription(request.description());
            existing.setIsActive(true);
            tenantSecretRepository.save(existing);
            log.info("Tenant secret updated: tenantId={}, type={}", tenantId, request.secretType());
        } else {
            TenantSecret secret = TenantSecret.builder()
                    .tenantId(tenantId)
                    .secretType(request.secretType())
                    .secretValue(request.secretValue())
                    .description(request.description())
                    .isActive(true)
                    .build();
            existing = tenantSecretRepository.save(secret);
            log.info("Tenant secret created: tenantId={}, type={}", tenantId, request.secretType());
        }

        auditService.log(
                AuditAction.TENANT_SECRET,
                TENANT_SECRET,
                existing.getId(),
                "Upsert secret type=" + request.secretType() + " for tenant=" + tenantId
        );

        return toResponse(existing);
    }

    @Override
    public SecretResponse update(UUID tenantId, UUID secretId, UpdateSecretRequest request) {
        TenantSecret secret = tenantSecretRepository.findById(secretId)
                .orElseThrow(() -> new ResourceNotFoundException(SECRET_NOT_FOUND + secretId));

        if (!secret.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Secret not found for this tenant: " + secretId);
        }

        secret.setSecretValue(request.secretValue());
        if (request.description() != null) {
            secret.setDescription(request.description());
        }
        tenantSecretRepository.save(secret);

        log.info("Tenant secret updated: secretId={}, tenantId={}", secretId, tenantId);
        auditService.log(
                AuditAction.TENANT_SECRET,
                TENANT_SECRET,
                secretId,
                "Update secret for tenant=" + tenantId
        );

        return toResponse(secret);
    }

    @Override
    public void delete(UUID tenantId, UUID secretId) {
        TenantSecret secret = tenantSecretRepository.findById(secretId)
                .orElseThrow(() -> new ResourceNotFoundException(SECRET_NOT_FOUND + secretId));

        if (!secret.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Secret not found for this tenant: " + secretId);
        }

        tenantSecretRepository.delete(secret);
        log.info("Tenant secret deleted: secretId={}, tenantId={}", secretId, tenantId);
        auditService.log(
                AuditAction.TENANT_SECRET,
                TENANT_SECRET,
                secretId,
                "Delete secret type=" + secret.getSecretType() + " for tenant=" + tenantId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SecretResponse findById(UUID tenantId, UUID secretId) {
        TenantSecret secret = tenantSecretRepository.findById(secretId)
                .orElseThrow(() -> new ResourceNotFoundException(SECRET_NOT_FOUND + secretId));
        if (!secret.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Secret not found for this tenant: " + secretId);
        }
        return toResponse(secret);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecretResponse> listAll(UUID tenantId) {
        return tenantSecretRepository.findAllByTenantId(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getRawValue(UUID tenantId, SecretType type) {
        return tenantSecretRepository
                .findByTenantIdAndSecretType(tenantId, type)
                .filter(TenantSecret::getIsActive)
                .map(TenantSecret::getSecretValue);
    }

    private SecretResponse toResponse(TenantSecret secret) {
        String masked = maskValue(secret.getSecretValue());
        return new SecretResponse(
                secret.getId(),
                secret.getSecretType(),
                masked,
                secret.getDescription(),
                Boolean.TRUE.equals(secret.getIsActive()),
                secret.getCreatedAt(),
                secret.getUpdatedAt()
        );
    }

    static String maskValue(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return "****" + value.substring(value.length() - 4);
    }
}
