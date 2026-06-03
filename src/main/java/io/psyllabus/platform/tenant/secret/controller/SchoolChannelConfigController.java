package io.psyllabus.platform.tenant.secret.controller;

import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.tenant.secret.dto.request.ChannelConfigUpdateRequest;
import io.psyllabus.platform.tenant.secret.dto.response.ChannelConfigResponse;
import io.psyllabus.platform.tenant.secret.service.TenantChannelConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/schools/me/channel-config")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "School Channel Config", description = "School-admin channel config — ADMIN only")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class SchoolChannelConfigController extends BaseController {

    private final TenantChannelConfigService channelConfigService;

    @GetMapping
    @Operation(summary = "Get own channel config")
    public ResponseEntity<ApiResponse<ChannelConfigResponse>> getConfig() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return ok(channelConfigService.getConfig(tenantId), "Channel config retrieved");
    }

    @PatchMapping
    @Operation(summary = "Update own channel config", description = "Patch — only provided fields are updated")
    public ResponseEntity<ApiResponse<ChannelConfigResponse>> updateConfig(
            @Valid @RequestBody ChannelConfigUpdateRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return ok(channelConfigService.updateConfig(tenantId, request), "Channel config updated");
    }
}
