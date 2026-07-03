package com.baas.flashsale.auth.dto;

import com.baas.flashsale.tenant.entity.UserRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresInMs;
    private Long refreshExpiresInMs;
    private Long userId;
    private Long tenantId;
    private String tenantCode;
    private String username;
    private String email;
    private String fullName;
    private UserRole role;
}
