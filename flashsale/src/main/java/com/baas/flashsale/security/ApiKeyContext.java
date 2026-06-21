package com.baas.flashsale.security;

import com.baas.flashsale.tenant.entity.ApiKey;
import com.baas.flashsale.tenant.entity.Tenant;
import lombok.Getter;

@Getter
public class ApiKeyContext {
    private final ApiKey apiKey;
    private final Tenant tenant;

    public ApiKeyContext(ApiKey apiKey) {
        this.apiKey = apiKey;
        this.tenant = apiKey.getTenant();
    }
}
