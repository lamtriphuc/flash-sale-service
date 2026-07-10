package com.lamtriphuc.backend.order.controller;

import com.lamtriphuc.backend.common.dto.ApiResponse;
import com.lamtriphuc.backend.order.dto.CheckoutRequest;
import com.lamtriphuc.backend.order.service.CheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class CheckoutController {
    private final CheckoutService checkoutService;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<Void>> checkout(@Valid @RequestBody CheckoutRequest request) {
        checkoutService.processCheckout(request);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(null));
    }
}
