package io.psyllabus.platform.tenant.secret.service;

import io.psyllabus.platform.tenant.secret.dto.request.CreateSecretRequest;
import io.psyllabus.platform.tenant.secret.dto.request.UpdateSecretRequest;
import io.psyllabus.platform.tenant.secret.dto.response.SecretResponse;
import io.psyllabus.platform.tenant.secret.enums.SecretType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantSecretService {

    SecretResponse create(UUID tenantId, CreateSecretRequest request);

    SecretResponse update(UUID tenantId, UUID secretId, UpdateSecretRequest request);

    void delete(UUID tenantId, UUID secretId);

    SecretResponse findById(UUID tenantId, UUID secretId);

    List<SecretResponse> listAll(UUID tenantId);

    Optional<String> getRawValue(UUID tenantId, SecretType type);
}
