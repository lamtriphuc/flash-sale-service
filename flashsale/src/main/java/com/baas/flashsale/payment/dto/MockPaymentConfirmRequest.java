package com.baas.flashsale.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MockPaymentConfirmRequest {
    @NotNull(message = "Payment result is required")
    private MockPaymentResult result;
}
