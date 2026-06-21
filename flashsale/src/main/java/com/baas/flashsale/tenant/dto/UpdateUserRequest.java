package com.baas.flashsale.tenant.dto;

import com.baas.flashsale.tenant.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotNull(message = "Role is required")
    private UserRole role;

    @NotNull(message = "Active is required")
    private Boolean active;
}