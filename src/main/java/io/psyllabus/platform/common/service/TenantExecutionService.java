package io.psyllabus.platform.common.service;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Runs code inside an explicit tenant context outside the HTTP filter chain.
 *
 * <p>Used by async consumers and other background jobs that need the same
 * tenant-scoped service behavior as request-driven flows.</p>
 */
public interface TenantExecutionService {

    void runInTenant(UUID tenantId, Runnable action);

    <T> T supplyInTenant(UUID tenantId, Supplier<T> supplier);
}
