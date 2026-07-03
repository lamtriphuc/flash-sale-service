package com.baas.flashsale.tenant.service;

import com.baas.flashsale.common.BusinessException;
import com.baas.flashsale.tenant.dto.CreateTenantRequest;
import com.baas.flashsale.tenant.dto.TenantResponse;
import com.baas.flashsale.tenant.entity.Tenant;
import com.baas.flashsale.tenant.entity.User;
import com.baas.flashsale.tenant.entity.UserRole;
import com.baas.flashsale.tenant.repository.TenantRepository;
import com.baas.flashsale.tenant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TenantService {
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantResponse createTenant(CreateTenantRequest request) {
        if (request.getAdminUsername() == null || request.getAdminUsername().isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Admin username is required");
        }
        if (request.getAdminPassword() == null || request.getAdminPassword().isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Admin password is required");
        }

        if (tenantRepository.existsByCode(request.getCode())) {
            throw new BusinessException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Tenant code already exists");
        }

        Tenant tenant = Tenant.builder()
                .code(request.getCode().trim().toUpperCase())
                .name(request.getName().trim())
                .contactEmail(request.getContactEmail())
                .active(true)
                .build();

        Tenant savedTenant = tenantRepository.save(tenant);

        String adminEmail = request.getAdminUsername().trim().toLowerCase();
        User owner = User.builder()
                .tenant(savedTenant)
                .username(adminEmail)
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(request.getAdminPassword()))
                .fullName(request.getAdminUsername().trim())
                .role(UserRole.ADMIN)
                .active(true)
                .build();
        User savedOwner = userRepository.save(owner);

        return mapToTenantResponse(savedTenant, savedOwner.getId());
    }

    public List<TenantResponse> getAllTenants() {
        return tenantRepository.findAll()
                .stream()
                .map(this::mapToTenantResponse)
                .toList();
    }

    public TenantResponse getTenantById(Long currentTenantId) {
        Tenant tenant = findTenantById(currentTenantId);
        return mapToTenantResponse(tenant);
    }

    public TenantResponse updateTenant(Long currentTenantId, CreateTenantRequest request) {
        Tenant tenant = findTenantById(currentTenantId);

        if (!tenant.getCode().equalsIgnoreCase(request.getCode())
                && tenantRepository.existsByCode(request.getCode())) {
            throw new BusinessException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Tenant code already exists");
        }

        tenant.setCode(request.getCode().trim().toUpperCase());
        tenant.setName(request.getName().trim());
        tenant.setContactEmail(request.getContactEmail());

        return mapToTenantResponse(tenantRepository.save(tenant));
    }

    public void deactivateTenant(Long currentTenantId) {
        Tenant tenant = findTenantById(currentTenantId);
        tenant.setActive(false);
        tenantRepository.save(tenant);
    }

    // Utils
    private Tenant findTenantById(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new BusinessException("FORBIDDEN_RESOURCE", HttpStatus.FORBIDDEN, "Tenant not found"));
    }

    private TenantResponse mapToTenantResponse(Tenant tenant) {
        return mapToTenantResponse(tenant, null);
    }

    private TenantResponse mapToTenantResponse(Tenant tenant, Long ownerUserId) {
        return TenantResponse.builder()
                .id(tenant.getId())
                .code(tenant.getCode())
                .name(tenant.getName())
                .contactEmail(tenant.getContactEmail())
                .active(tenant.getActive())
                .createdAt(tenant.getCreatedAt())
                .ownerUserId(ownerUserId)
                .build();
    }

}
