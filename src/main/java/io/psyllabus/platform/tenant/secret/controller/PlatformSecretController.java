package io.psyllabus.platform.tenant.secret.controller;

import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.common.dto.ApiResponse;
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
@RequestMapping("/api/platform/schools/{schoolId}/secrets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Platform Secrets", description = "Cross-tenant secret management — PLATFORM_ADMIN only")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformSecretController extends BaseController {

    private final TenantSecretService tenantSecretService;

    @GetMapping
    @Operation(summary = "List all secrets for a school", description = "Returns secrets with masked values")
    public ResponseEntity<ApiResponse<List<SecretResponse>>> listSecrets(@PathVariable UUID schoolId) {
        return ok(tenantSecretService.listAll(schoolId), "Secrets retrieved");
    }

    @PostMapping
    @Operation(summary = "Create or update a secret", description = "Upsert by tenant+type")
    public ResponseEntity<ApiResponse<SecretResponse>> createSecret(
            @PathVariable UUID schoolId,
            @Valid @RequestBody CreateSecretRequest request) {
        return created(tenantSecretService.create(schoolId, request), "Secret created");
    }

    @GetMapping("/{secretId}")
    @Operation(summary = "Get a single secret", description = "Returns masked value")
    public ResponseEntity<ApiResponse<SecretResponse>> getSecret(
            @PathVariable UUID schoolId,
            @PathVariable UUID secretId) {
        return ok(tenantSecretService.findById(schoolId, secretId), "Secret retrieved");
    }

    @PutMapping("/{secretId}")
    @Operation(summary = "Update a secret value")
    public ResponseEntity<ApiResponse<SecretResponse>> updateSecret(
            @PathVariable UUID schoolId,
            @PathVariable UUID secretId,
            @Valid @RequestBody UpdateSecretRequest request) {
        return ok(tenantSecretService.update(schoolId, secretId, request), "Secret updated");
    }

    @DeleteMapping("/{secretId}")
    @Operation(summary = "Delete a secret")
    public ResponseEntity<ApiResponse<Void>> deleteSecret(
            @PathVariable UUID schoolId,
            @PathVariable UUID secretId) {
        tenantSecretService.delete(schoolId, secretId);
        return okMessage("Secret deleted");
    }
}
