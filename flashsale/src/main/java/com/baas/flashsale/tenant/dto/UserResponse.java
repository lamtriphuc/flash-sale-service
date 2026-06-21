package com.baas.flashsale.tenant.dto;

import com.baas.flashsale.tenant.entity.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private Long tenantId;
    private String tenantCode;
    private String username;
    private String fullName;
    private UserRole role;
    private Boolean active;
    private LocalDateTime createdAt;
}