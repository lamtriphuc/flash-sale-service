package com.baas.flashsale.tenant.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TenantResponse {
    private Long id;
    private String code;
    private String name;
    private String contactEmail;
    private Boolean active;
    private LocalDateTime createdAt;
    private Long ownerUserId;
}
