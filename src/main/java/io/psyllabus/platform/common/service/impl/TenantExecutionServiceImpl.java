package io.psyllabus.platform.common.service.impl;

import io.psyllabus.platform.common.service.TenantExecutionService;
import io.psyllabus.platform.common.util.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Background-job helper for establishing and clearing {@link TenantContext}.
 */
@Service
@Slf4j
public class TenantExecutionServiceImpl implements TenantExecutionService {

    @Override
    public void runInTenant(UUID tenantId, Runnable action) {
        UUID previousTenant = TenantContext.getCurrentTenantId();
        TenantContext.setCurrentTenant(tenantId);
        try {
            action.run();
        } finally {
            restore(previousTenant);
        }
    }

    @Override
    public <T> T supplyInTenant(UUID tenantId, Supplier<T> supplier) {
        UUID previousTenant = TenantContext.getCurrentTenantId();
        TenantContext.setCurrentTenant(tenantId);
        try {
            return supplier.get();
        } finally {
            restore(previousTenant);
        }
    }

    private void restore(UUID previousTenant) {
        if (previousTenant == null) {
            TenantContext.clear();
        } else {
            TenantContext.setCurrentTenant(previousTenant);
        }
    }
}
