package io.psyllabus.platform.tenant.secret.service;

import com.resend.Resend;
import io.psyllabus.platform.tenant.secret.enums.SecretType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class TenantResendFactory {

    private final TenantSecretService tenantSecretService;
    private final Resend platformResendClient;

    private final Map<UUID, Resend> clientCache = new ConcurrentHashMap<>();

    public TenantResendFactory(TenantSecretService tenantSecretService,
                                ObjectProvider<Resend> platformResendProvider) {
        this.tenantSecretService = tenantSecretService;
        this.platformResendClient = platformResendProvider.getIfAvailable();
        if (this.platformResendClient == null) {
            log.warn("Platform Resend client not available — platform-track emails will fail");
        }
    }

    @PostConstruct
    void logInit() {
        log.info("TenantResendFactory initialized");
    }

    public Optional<Resend> getClient(UUID tenantId) {
        return Optional.ofNullable(clientCache.computeIfAbsent(tenantId, this::createClient));
    }

    public Resend getPlatformClient() {
        return platformResendClient;
    }

    private Resend createClient(UUID tenantId) {
        return tenantSecretService.getRawValue(tenantId, SecretType.RESEND_API_KEY)
                .map(key -> {
                    log.debug("Creating Resend client for tenant={}", tenantId);
                    return new Resend(key);
                })
                .orElse(null);
    }

    public void evict(UUID tenantId) {
        Resend removed = clientCache.remove(tenantId);
        if (removed != null) {
            log.debug("Evicted Resend client cache for tenant={}", tenantId);
        }
    }
}
