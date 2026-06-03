package io.psyllabus.platform.tenant.secret.controller;

import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.common.dto.ApiResponse;
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
@RequestMapping("/api/platform/schools/{schoolId}/channel-config")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Platform Channel Config", description = "Cross-tenant channel config — PLATFORM_ADMIN only")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformChannelConfigController extends BaseController {

    private final TenantChannelConfigService channelConfigService;

    @GetMapping
    @Operation(summary = "Get channel config for a school")
    public ResponseEntity<ApiResponse<ChannelConfigResponse>> getConfig(@PathVariable UUID schoolId) {
        return ok(channelConfigService.getConfig(schoolId), "Channel config retrieved");
    }

    @PatchMapping
    @Operation(summary = "Update channel config", description = "Patch — only provided fields are updated")
    public ResponseEntity<ApiResponse<ChannelConfigResponse>> updateConfig(
            @PathVariable UUID schoolId,
            @Valid @RequestBody ChannelConfigUpdateRequest request) {
        return ok(channelConfigService.updateConfig(schoolId, request), "Channel config updated");
    }
}
