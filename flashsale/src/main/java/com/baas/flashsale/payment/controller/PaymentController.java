package com.baas.flashsale.payment.controller;

import com.baas.flashsale.flashsale.dto.OrderResponse;
import com.baas.flashsale.payment.dto.MockPaymentConfirmRequest;
import com.baas.flashsale.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/{orderId}/mock-confirm")
    public OrderResponse mockConfirm(
            @PathVariable Long orderId,
            @Valid @RequestBody MockPaymentConfirmRequest request
    ) {
        return paymentService.mockConfirm(orderId, request);
    }
}
