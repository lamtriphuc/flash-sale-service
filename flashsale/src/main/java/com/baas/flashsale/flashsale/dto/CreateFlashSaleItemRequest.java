package com.baas.flashsale.flashsale.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateFlashSaleItemRequest {
    @NotBlank(message = "Item code is required")
    private String itemCode;

    @NotBlank(message = "Item name is required")
    private String itemName;

    @NotNull(message = "Original price is required")
    @Min(value = 1, message = "Original price must be positive")
    private Long originalPrice;

    @NotNull(message = "Sale price is required")
    @Min(value = 1, message = "Sale price must be positive")
    private Long salePrice;

    @NotNull(message = "Total quantity is required")
    @Min(value = 1, message = "Total quantity must be positive")
    private Integer totalQuantity;
}
