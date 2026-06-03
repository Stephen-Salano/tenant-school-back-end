package io.psyllabus.platform.tenant.secret.service;

import io.psyllabus.platform.tenant.secret.dto.request.ChannelConfigUpdateRequest;
import io.psyllabus.platform.tenant.secret.dto.response.ChannelConfigResponse;
import io.psyllabus.platform.tenant.secret.entity.TenantChannelConfig;
import io.psyllabus.platform.tenant.secret.enums.SecretType;
import io.psyllabus.platform.tenant.secret.repository.TenantChannelConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TenantChannelConfigServiceImpl implements TenantChannelConfigService {

    private final TenantChannelConfigRepository channelConfigRepository;
    private final TenantSecretService tenantSecretService;

    @Override
    public void createDefault(UUID tenantId) {
        if (channelConfigRepository.existsById(tenantId)) {
            log.debug("Channel config already exists for tenant={}, skipping creation", tenantId);
            return;
        }
        TenantChannelConfig config = TenantChannelConfig.builder()
                .tenantId(tenantId)
                .emailEnabled(true)
                .smsEnabled(false)
                .build();
        channelConfigRepository.save(config);
        log.info("Default channel config created for tenant={}", tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "channelConfig", key = "#tenantId", unless = "#result == null")
    public ChannelConfigResponse getConfig(UUID tenantId) {
        TenantChannelConfig config = channelConfigRepository.findById(tenantId)
                .orElse(null);

        if (config == null) {
            log.warn("Channel config not found for tenant={}, returning defaults", tenantId);
            return new ChannelConfigResponse(
                    tenantId, true, false, null, null,
                    hasSecret(tenantId, SecretType.RESEND_API_KEY),
                    hasSecret(tenantId, SecretType.AT_API_KEY),
                    null
            );
        }

        return new ChannelConfigResponse(
                config.getTenantId(),
                Boolean.TRUE.equals(config.getEmailEnabled()),
                Boolean.TRUE.equals(config.getSmsEnabled()),
                config.getEmailFromName(),
                config.getEmailReplyTo(),
                hasSecret(tenantId, SecretType.RESEND_API_KEY),
                hasSecret(tenantId, SecretType.AT_API_KEY),
                config.getUpdatedAt()
        );
    }

    @Override
    @CacheEvict(value = "channelConfig", key = "#tenantId")
    public ChannelConfigResponse updateConfig(UUID tenantId, ChannelConfigUpdateRequest request) {
        TenantChannelConfig config = channelConfigRepository.findById(tenantId)
                .orElseGet(() -> {
                    log.info("Channel config not found for tenant={}, creating before update", tenantId);
                    TenantChannelConfig c = TenantChannelConfig.builder()
                            .tenantId(tenantId)
                            .emailEnabled(true)
                            .smsEnabled(false)
                            .build();
                    return channelConfigRepository.save(c);
                });

        if (request.emailEnabled() != null) config.setEmailEnabled(request.emailEnabled());
        if (request.smsEnabled() != null) config.setSmsEnabled(request.smsEnabled());
        if (request.emailFromName() != null) config.setEmailFromName(request.emailFromName());
        if (request.emailReplyTo() != null) config.setEmailReplyTo(request.emailReplyTo());

        channelConfigRepository.save(config);
        log.info("Channel config updated for tenant={}", tenantId);

        return new ChannelConfigResponse(
                config.getTenantId(),
                Boolean.TRUE.equals(config.getEmailEnabled()),
                Boolean.TRUE.equals(config.getSmsEnabled()),
                config.getEmailFromName(),
                config.getEmailReplyTo(),
                hasSecret(tenantId, SecretType.RESEND_API_KEY),
                hasSecret(tenantId, SecretType.AT_API_KEY),
                config.getUpdatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailEnabled(UUID tenantId) {
        return channelConfigRepository.findById(tenantId)
                .map(TenantChannelConfig::getEmailEnabled)
                .orElse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSmsEnabled(UUID tenantId) {
        return channelConfigRepository.findById(tenantId)
                .map(TenantChannelConfig::getSmsEnabled)
                .orElse(false);
    }

    private boolean hasSecret(UUID tenantId, SecretType type) {
        return tenantSecretService.getRawValue(tenantId, type).isPresent();
    }
}
