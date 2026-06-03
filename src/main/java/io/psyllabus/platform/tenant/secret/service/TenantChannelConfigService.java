package io.psyllabus.platform.tenant.secret.service;

import io.psyllabus.platform.tenant.secret.dto.request.ChannelConfigUpdateRequest;
import io.psyllabus.platform.tenant.secret.dto.response.ChannelConfigResponse;
import java.util.UUID;

public interface TenantChannelConfigService {

    void createDefault(UUID tenantId);

    ChannelConfigResponse getConfig(UUID tenantId);

    ChannelConfigResponse updateConfig(UUID tenantId, ChannelConfigUpdateRequest request);

    boolean isEmailEnabled(UUID tenantId);

    boolean isSmsEnabled(UUID tenantId);
}
