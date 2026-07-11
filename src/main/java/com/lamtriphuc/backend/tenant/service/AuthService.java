package com.lamtriphuc.backend.tenant.service;

import com.lamtriphuc.backend.common.exception.AppException;
import com.lamtriphuc.backend.common.exception.ErrorCode;
import com.lamtriphuc.backend.common.security.JwtUtil;
import com.lamtriphuc.backend.tenant.dto.AuthResponse;
import com.lamtriphuc.backend.tenant.dto.LoginRequest;
import com.lamtriphuc.backend.tenant.dto.RegisterRequest;
import com.lamtriphuc.backend.tenant.entity.ApiKey;
import com.lamtriphuc.backend.tenant.entity.Tenant;
import com.lamtriphuc.backend.tenant.entity.TenantAccount;
import com.lamtriphuc.backend.tenant.repository.ApiKeyRepository;
import com.lamtriphuc.backend.tenant.repository.TenantAccountRepository;
import com.lamtriphuc.backend.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final TenantRepository tenantRepository;
    private final TenantAccountRepository tenantAccountRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (tenantAccountRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        Tenant tenant = Tenant.builder()
                .name(request.getCompanyName())
                .status("ACTIVE")
                .build();
        tenant = tenantRepository.save(tenant);

        // Tạo tài khoản Admin mặc định cho công ty đó
        TenantAccount account = TenantAccount.builder()
                .tenant(tenant)
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("ADMIN")
                .build();
        account = tenantAccountRepository.save(account);

        // Tạo Public Key
        String rawPublicKey = "pk_live_" + generateRandomString(32);
        ApiKey publicKeyEntity = ApiKey.builder()
                .tenant(tenant)
                .keyType("PUBLIC")
                .apiKeyHash(hashApiKey(rawPublicKey))
                .prefix("pk_live_")
                .isActive(true)
                .build();

        // Tạo Secret Key
        String rawSecretKey = "sk_live_" + generateRandomString(32);
        ApiKey secretKeyEntity = ApiKey.builder()
                .tenant(tenant)
                .keyType("SECRET")
                .apiKeyHash(hashApiKey(rawSecretKey))
                .prefix("sk_live_")
                .isActive(true)
                .build();

        // Lưu vào DB
        apiKeyRepository.saveAll(List.of(publicKeyEntity, secretKeyEntity));

        String token = jwtUtil.generateToken(account.getId(), tenant.getId(), account.getEmail(), account.getRole());

        return AuthResponse.builder()
                .accessToken(token)
                .companyName(tenant.getName())
                .role(account.getRole())
                .publicKey(rawPublicKey)
                .secretKey(rawSecretKey)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        TenantAccount account = tenantAccountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if ("SUSPENDED".equals(account.getTenant().getStatus())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String token = jwtUtil.generateToken(account.getId(), account.getTenant().getId(), account.getEmail(), account.getRole());

        return AuthResponse.builder()
                .accessToken(token)
                .companyName(account.getTenant().getName())
                .role(account.getRole())
                .build();
    }


    // Helper
    // Hàm tạo chuỗi ngẫu nhiên (chạy 1 lần)
    private String generateRandomString(int byteLength) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] token = new byte[byteLength];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    // Hàm Hash bằng SHA-256 (Để lưu vào DB và để tra cứu sau này)
    private String hashApiKey(String rawApiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawApiKey.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Lỗi thuật toán Hash", e);
        }
    }
}
