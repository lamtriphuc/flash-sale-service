package com.baas.flashsale.security;

import com.baas.flashsale.common.BusinessException;
import com.baas.flashsale.tenant.entity.Tenant;
import org.springframework.http.HttpStatus;

public final class CurrentTenant {
    private static final ThreadLocal<Tenant> TENANT = new ThreadLocal<>();

    private CurrentTenant() {
    }

    public static void set(Tenant tenant) {
        TENANT.set(tenant);
    }

    public static Tenant get() {
        Tenant tenant = TENANT.get();
        if (tenant == null) {
            throw new BusinessException("INVALID_API_KEY", HttpStatus.UNAUTHORIZED, "Missing tenant context");
        }
        return tenant;
    }

    public static Long getId() {
        return get().getId();
    }

    public static void clear() {
        TENANT.remove();
    }
}
