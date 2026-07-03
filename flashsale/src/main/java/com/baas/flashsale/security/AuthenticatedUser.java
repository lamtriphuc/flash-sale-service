package com.baas.flashsale.security;

import com.baas.flashsale.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthenticatedUser {
    private AuthenticatedUser() {
    }

    public static TenantUserDetails get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof TenantUserDetails userDetails)) {
            throw new BusinessException("INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return userDetails;
    }

    public static Long tenantId() {
        return get().getTenantId();
    }
}
