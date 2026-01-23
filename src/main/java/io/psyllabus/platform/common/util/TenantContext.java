package io.psyllabus.platform.common.util;

import java.util.UUID;

/**
 * Manages current tenant context using ThreadLocal.
 * CRITICAL: Always set tenant_id from JWT, never from request.
 *
 * a tiny utility that keeps track of the current tenant ID while a request is being processed — per thread.
 */
public class TenantContext {
    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    public static void setCurrentTenant(UUID tenantId){
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getCurrentTenantId(){
        return CURRENT_TENANT.get();
    }

    public static void clear(){
        CURRENT_TENANT.remove();
    }
}
