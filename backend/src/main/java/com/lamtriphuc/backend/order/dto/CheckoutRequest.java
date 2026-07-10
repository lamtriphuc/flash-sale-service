package com.lamtriphuc.backend.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CheckoutRequest {
    @NotNull(message = "ID Sản phẩm không được để trống")
    private UUID productId;

    @Min(value = 1, message = "Số lượng mua ít nhất là 1")
    private Integer quantity;

    @NotBlank(message = "Thiếu khóa chống trùng lặp (Idempotency Key)")
    private String idempotencyKey;
}