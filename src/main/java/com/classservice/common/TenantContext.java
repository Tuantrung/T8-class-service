package com.classservice.common;

import java.util.UUID;

/**
 * ThreadLocal holder for the current request's tenant ID.
 * Set by TenantContextFilter; cleared in the same filter's finally block
 * to prevent leakage on thread reuse.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CONTEXT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID tenantId) {
        CONTEXT.set(tenantId);
    }

    public static UUID get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
