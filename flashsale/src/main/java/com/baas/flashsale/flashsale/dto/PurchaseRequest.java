package com.baas.flashsale.flashsale.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PurchaseRequest {
    @NotBlank(message = "User id is required")
    private String userId;
}
