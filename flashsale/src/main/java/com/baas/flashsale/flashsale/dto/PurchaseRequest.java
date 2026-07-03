package com.baas.flashsale.flashsale.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PurchaseRequest {
    @NotNull(message = "Item id is required")
    private Long itemId;
}
