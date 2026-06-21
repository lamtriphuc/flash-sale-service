package com.baas.flashsale.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateApiKeyRequest {
    @NotBlank(message = "API key name is required")
    private String name;

    private LocalDateTime expiredAt;
}