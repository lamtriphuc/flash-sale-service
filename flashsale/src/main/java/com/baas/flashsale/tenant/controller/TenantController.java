package com.baas.flashsale.tenant.controller;

import com.baas.flashsale.tenant.dto.ApiKeyResponse;
import com.baas.flashsale.tenant.dto.CreateApiKeyRequest;
import com.baas.flashsale.tenant.dto.CreateTenantRequest;
import com.baas.flashsale.tenant.dto.TenantResponse;
import com.baas.flashsale.tenant.service.TenantService;
import com.baas.flashsale.common.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.baas.flashsale.security.TenantUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {
    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(
            @Valid @RequestBody CreateTenantRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tenantService.createTenant(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public TenantResponse getCurrentTenant(@AuthenticationPrincipal TenantUserDetails currentUser) {
        return tenantService.getTenantById(currentUser.getTenantId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public TenantResponse getTenantById(
            @AuthenticationPrincipal TenantUserDetails currentUser,
            @PathVariable Long id
    ) {
        assertOwnTenant(currentUser, id);
        return tenantService.getTenantById(currentUser.getTenantId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public TenantResponse updateTenant(
            @AuthenticationPrincipal TenantUserDetails currentUser,
            @PathVariable Long id,
            @Valid @RequestBody CreateTenantRequest request
    ) {
        assertOwnTenant(currentUser, id);
        return tenantService.updateTenant(currentUser.getTenantId(), request);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('OWNER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateTenant(
            @AuthenticationPrincipal TenantUserDetails currentUser,
            @PathVariable Long id
    ) {
        assertOwnTenant(currentUser, id);
        tenantService.deactivateTenant(currentUser.getTenantId());
    }

    // API KEY ==========================================

    @PostMapping("/{tenantId}/api-keys")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiKeyResponse> createApiKey(
            @AuthenticationPrincipal TenantUserDetails currentUser,
            @PathVariable Long tenantId,
            @Valid @RequestBody CreateApiKeyRequest request
    ) {
        assertOwnTenant(currentUser, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(tenantService.createApiKey(currentUser.getTenantId(), request));
    }

    @GetMapping("/{tenantId}/api-keys")
    @PreAuthorize("hasRole('OWNER')")
    public List<ApiKeyResponse> getApiKeysByTenant(
            @AuthenticationPrincipal TenantUserDetails currentUser,
            @PathVariable Long tenantId
    ) {
        assertOwnTenant(currentUser, tenantId);
        return tenantService.getApiKeysByTenant(currentUser.getTenantId());
    }

    @PatchMapping("/api-keys/{apiKeyId}/deactivate")
    @PreAuthorize("hasRole('OWNER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateApiKey(
            @AuthenticationPrincipal TenantUserDetails currentUser,
            @PathVariable Long apiKeyId
    ) {
        tenantService.deactivateApiKey(currentUser.getTenantId(), apiKeyId);
    }

    private void assertOwnTenant(TenantUserDetails currentUser, Long tenantId) {
        if (!currentUser.getTenantId().equals(tenantId)) {
            throw new BusinessException("FORBIDDEN_RESOURCE", HttpStatus.FORBIDDEN, "Tenant resource is forbidden");
        }
    }
}
