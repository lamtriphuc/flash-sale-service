package com.baas.flashsale.flashsale.controller;

import com.baas.flashsale.flashsale.service.PurchaseService;
import com.baas.flashsale.flashsale.dto.OrderResponse;
import com.baas.flashsale.flashsale.dto.PurchaseRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
public class PurchaseController {
    private final PurchaseService purchaseService;

    @PostMapping("/{campaignId}/orders")
    public ResponseEntity<OrderResponse> createOrder(
            @PathVariable Long campaignId,
            @Valid @RequestBody PurchaseRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseService.createOrder(campaignId, request));
    }

    @GetMapping("/{campaignId}/orders")
    public List<OrderResponse> getOrders(
            @PathVariable Long campaignId,
            java.security.Principal principal
    ) {
        return purchaseService.getOrders(campaignId);
    }
}
