package com.baas.flashsale.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTenantRequest {
    @NotBlank(message = "Tenant code is required")
    private String code;

    @NotBlank(message = "Tenant name is required")
    private String name;

    @Email(message = "Invalid email")
    private String contactEmail;

    private String adminUsername;

    private String adminPassword;
}
