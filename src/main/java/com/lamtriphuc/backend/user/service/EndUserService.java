package com.lamtriphuc.backend.user.service;

import com.lamtriphuc.backend.common.security.CryptoUtils;
import com.lamtriphuc.backend.common.security.JwtUtil;
import com.lamtriphuc.backend.common.security.SecurityUtils;
import com.lamtriphuc.backend.user.dto.EndUserIdentifyRequest;
import com.lamtriphuc.backend.user.dto.EndUserIdentifyResponse;
import com.lamtriphuc.backend.user.entity.EndUser;
import com.lamtriphuc.backend.user.repository.EndUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EndUserService {
    private final EndUserRepository endUserRepository;
    private final CryptoUtils cryptoUtils;
    private final JwtUtil jwtUtil;

    @Transactional
    public EndUserIdentifyResponse identifyUser(
            EndUserIdentifyRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID currentTenantId = SecurityUtils.getCurrentTenantId();

        // 1. Tạo Blind Index để tìm kiếm
        String hashedIdentifier = cryptoUtils.hashIdentifier(request.getIdentifier());

        // 2. Tìm hoặc Tạo mới User
        EndUser endUser = endUserRepository
                .findByTenantIdAndIdentifierHash(currentTenantId, hashedIdentifier)
                .orElseGet(() -> {
                    // Nếu là khách mới, mã hóa thông tin và lưu vào DB
                    EndUser newUser = EndUser.builder()
                            .tenantId(currentTenantId)
                            .identifierHash(hashedIdentifier)
                            .encryptedProfile(cryptoUtils.encryptProfile(request.getIdentifier()))
                            .deviceFingerprint(request.getDeviceFingerprint())
                            .ipAddress(getClientIp(httpRequest))
                            .isBlocked(false)
                            .build();
                    return endUserRepository.save(newUser);
                });

        // (Anti-Cheat) Ở đây bạn có thể thêm logic:
        // Nếu endUser.getIsBlocked() == true -> Ném AppException "Tài khoản bị khóa"

        // 3. Cấp UserToken (Hạn rất ngắn, ví dụ 1 tiếng, role là END_USER)
        String userToken = jwtUtil.generateToken(
                endUser.getId(),
                currentTenantId,
                request.getIdentifier(),
                "END_USER"
        );

        // 4. Cấp Idempotency Key (Khóa lũy đẳng)
        String idempotencyKey = UUID.randomUUID().toString();

        return EndUserIdentifyResponse.builder()
                .userToken(userToken)
                .idempotencyKey(idempotencyKey)
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }
}
