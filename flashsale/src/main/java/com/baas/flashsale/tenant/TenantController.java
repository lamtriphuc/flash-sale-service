package com.baas.flashsale.tenant;

import com.baas.flashsale.tenant.dto.ApiKeyResponse;
import com.baas.flashsale.tenant.dto.CreateApiKeyRequest;
import com.baas.flashsale.tenant.dto.CreateTenantRequest;
import com.baas.flashsale.tenant.dto.TenantResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {
    private final TenantService tenantService;

    @PostMapping
    public TenantResponse createTenant(
            @Valid @RequestBody CreateTenantRequest request
    ) {
        return tenantService.createTenant(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public List<TenantResponse> getAllTenants() {
        return tenantService.getAllTenants();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public TenantResponse getTenantById(@PathVariable Long id) {
        return tenantService.getTenantById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public TenantResponse updateTenant(
            @PathVariable Long id,
            @Valid @RequestBody CreateTenantRequest request
    ) {
        return tenantService.updateTenant(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('OWNER')")
    public void deactivateTenant(@PathVariable Long id) {
        tenantService.deactivateTenant(id);
    }

    // API KEY ==========================================

    @PostMapping("/{tenantId}/api-keys")
    @PreAuthorize("hasRole('OWNER')")
    public ApiKeyResponse createApiKey(
            @PathVariable Long tenantId,
            @Valid @RequestBody CreateApiKeyRequest request
    ) {
        return tenantService.createApiKey(tenantId, request);
    }

    @GetMapping("/{tenantId}/api-keys")
    @PreAuthorize("hasRole('OWNER')")
    public List<ApiKeyResponse> getApiKeysByTenant(@PathVariable Long tenantId) {
        return tenantService.getApiKeysByTenant(tenantId);
    }

    @PatchMapping("/api-keys/{apiKeyId}/deactivate")
    @PreAuthorize("hasRole('OWNER')")
    public void deactivateApiKey(@PathVariable Long apiKeyId) {
        tenantService.deactivateApiKey(apiKeyId);
    }
}
