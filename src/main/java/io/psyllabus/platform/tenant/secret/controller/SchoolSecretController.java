package io.psyllabus.platform.tenant.secret.controller;

import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.tenant.secret.dto.request.CreateSecretRequest;
import io.psyllabus.platform.tenant.secret.dto.request.UpdateSecretRequest;
import io.psyllabus.platform.tenant.secret.dto.response.SecretResponse;
import io.psyllabus.platform.tenant.secret.service.TenantSecretService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/me/secrets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "School Secrets", description = "School-admin secret management — ADMIN only")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class SchoolSecretController extends BaseController {

    private final TenantSecretService tenantSecretService;

    @GetMapping
    @Operation(summary = "List own secrets", description = "Returns secrets with masked values")
    public ResponseEntity<ApiResponse<List<SecretResponse>>> listSecrets() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return ok(tenantSecretService.listAll(tenantId), "Secrets retrieved");
    }

    @PostMapping
    @Operation(summary = "Create or update a secret", description = "Upsert by tenant+type")
    public ResponseEntity<ApiResponse<SecretResponse>> createSecret(
            @Valid @RequestBody CreateSecretRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return created(tenantSecretService.create(tenantId, request), "Secret created");
    }

    @GetMapping("/{secretId}")
    @Operation(summary = "Get a single secret", description = "Returns masked value")
    public ResponseEntity<ApiResponse<SecretResponse>> getSecret(@PathVariable UUID secretId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return ok(tenantSecretService.findById(tenantId, secretId), "Secret retrieved");
    }

    @PutMapping("/{secretId}")
    @Operation(summary = "Update a secret value")
    public ResponseEntity<ApiResponse<SecretResponse>> updateSecret(
            @PathVariable UUID secretId,
            @Valid @RequestBody UpdateSecretRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return ok(tenantSecretService.update(tenantId, secretId, request), "Secret updated");
    }

    @DeleteMapping("/{secretId}")
    @Operation(summary = "Delete a secret")
    public ResponseEntity<ApiResponse<Void>> deleteSecret(@PathVariable UUID secretId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        tenantSecretService.delete(tenantId, secretId);
        return okMessage("Secret deleted");
    }
}
