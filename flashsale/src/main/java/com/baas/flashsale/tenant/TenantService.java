package com.baas.flashsale.tenant;

import com.baas.flashsale.tenant.dto.ApiKeyResponse;
import com.baas.flashsale.tenant.dto.CreateApiKeyRequest;
import com.baas.flashsale.tenant.dto.CreateTenantRequest;
import com.baas.flashsale.tenant.dto.TenantResponse;
import com.baas.flashsale.tenant.entity.ApiKey;
import com.baas.flashsale.tenant.entity.Tenant;
import com.baas.flashsale.tenant.entity.User;
import com.baas.flashsale.tenant.entity.UserRole;
import com.baas.flashsale.tenant.repository.ApiKeyRepository;
import com.baas.flashsale.tenant.repository.TenantRepository;
import com.baas.flashsale.tenant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TenantService {
    private final TenantRepository tenantRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantResponse createTenant(CreateTenantRequest request) {
        if (request.getAdminUsername() == null || request.getAdminUsername().isBlank()) {
            throw new RuntimeException("Admin username is required");
        }
        if (request.getAdminPassword() == null || request.getAdminPassword().isBlank()) {
            throw new RuntimeException("Admin password is required");
        }

        if (tenantRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Tenant code already exists");
        }

        Tenant tenant = Tenant.builder()
                .code(request.getCode().trim().toUpperCase())
                .name(request.getName().trim())
                .contactEmail(request.getContactEmail())
                .active(true)
                .build();

        Tenant savedTenant = tenantRepository.save(tenant);

        User owner = User.builder()
                .tenant(savedTenant)
                .username(request.getAdminUsername().trim())
                .passwordHash(passwordEncoder.encode(request.getAdminPassword()))
                .fullName(request.getAdminUsername().trim())
                .role(UserRole.OWNER)
                .active(true)
                .build();
        User savedOwner = userRepository.save(owner);

        String rawApiKey = generateApiKey();
        ApiKey apiKey = ApiKey.builder()
                .tenant(savedTenant)
                .name("Default Key")
                .keyValue(hashApiKey(rawApiKey))
                .active(true)
                .build();
        apiKeyRepository.save(apiKey);

        return mapToTenantResponse(savedTenant, savedOwner.getId(), rawApiKey);
    }

    public List<TenantResponse> getAllTenants() {
        return tenantRepository.findAll()
                .stream()
                .map(this::mapToTenantResponse)
                .toList();
    }

    public TenantResponse getTenantById(Long id) {
        Tenant tenant = findTenantById(id);
        return mapToTenantResponse(tenant);
    }

    public TenantResponse updateTenant(Long id, CreateTenantRequest request) {
        Tenant tenant = findTenantById(id);

        if (!tenant.getCode().equalsIgnoreCase(request.getCode())
                && tenantRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Tenant code already exists");
        }

        tenant.setCode(request.getCode().trim().toUpperCase());
        tenant.setName(request.getName().trim());
        tenant.setContactEmail(request.getContactEmail());

        return mapToTenantResponse(tenantRepository.save(tenant));
    }

    public void deactivateTenant(Long id) {
        Tenant tenant = findTenantById(id);
        tenant.setActive(false);
        tenantRepository.save(tenant);
    }

    public ApiKeyResponse createApiKey(Long tenantId, CreateApiKeyRequest request) {
        Tenant tenant = findTenantById(tenantId);

        String rawApiKey = generateApiKey();

        ApiKey apiKey = ApiKey.builder()
                .tenant(tenant)
                .name(request.getName())
                .keyValue(hashApiKey(rawApiKey))
                .active(true)
                .expiredAt(request.getExpiredAt())
                .build();

        ApiKey savedApiKey = apiKeyRepository.save(apiKey);

        return mapToApiKeyResponse(savedApiKey, rawApiKey);
    }

    public List<ApiKeyResponse> getApiKeysByTenant(Long tenantId) {
        findTenantById(tenantId);

        return apiKeyRepository.findByTenantId(tenantId)
                .stream()
                .map(apiKey -> mapToApiKeyResponse(apiKey, null))
                .toList();
    }

    public void deactivateApiKey(Long apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new RuntimeException("API key not found"));

        apiKey.setActive(false);
        apiKeyRepository.save(apiKey);
    }


    // Utils
    private Tenant findTenantById(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
    }

    private TenantResponse mapToTenantResponse(Tenant tenant) {
        return mapToTenantResponse(tenant, null, null);
    }

    private TenantResponse mapToTenantResponse(Tenant tenant, Long ownerUserId, String rawApiKey) {
        return TenantResponse.builder()
                .id(tenant.getId())
                .code(tenant.getCode())
                .name(tenant.getName())
                .contactEmail(tenant.getContactEmail())
                .active(tenant.getActive())
                .createdAt(tenant.getCreatedAt())
                .ownerUserId(ownerUserId)
                .apiKey(rawApiKey)
                .build();
    }

    private ApiKeyResponse mapToApiKeyResponse(ApiKey apiKey, String rawApiKey) {
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .tenantId(apiKey.getTenant().getId())
                .name(apiKey.getName())
                .keyValue(rawApiKey)
                .active(apiKey.getActive())
                .expiredAt(apiKey.getExpiredAt())
                .createdAt(apiKey.getCreatedAt())
                .build();
    }

    private String generateApiKey() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);

        return "fs_" + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private String hashApiKey(String rawApiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawApiKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
