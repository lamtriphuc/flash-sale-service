package com.baas.flashsale.tenant.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ApiKeyResponse {
    private Long id;
    private Long tenantId;
    private String name;
    private String keyValue;
    private Boolean active;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;
}
