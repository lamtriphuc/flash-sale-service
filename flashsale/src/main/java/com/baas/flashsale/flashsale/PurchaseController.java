package com.baas.flashsale.flashsale;

import com.baas.flashsale.flashsale.dto.OrderResponse;
import com.baas.flashsale.flashsale.dto.PurchaseRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
public class PurchaseController {
    private final PurchaseService purchaseService;

    @PostMapping("/{campaignId}/items/{itemId}/purchase")
    public OrderResponse purchase(
            @RequestHeader("X-API-Key") String apiKey,
            @PathVariable Long campaignId,
            @PathVariable Long itemId,
            @Valid @RequestBody PurchaseRequest request
    ) {
        return purchaseService.purchase(apiKey, campaignId, itemId, request);
    }

    @GetMapping("/{campaignId}/orders")
    public List<OrderResponse> getOrders(
            @RequestHeader("X-API-Key") String apiKey,
            @PathVariable Long campaignId,
            @RequestParam String userId
    ) {
        return purchaseService.getOrders(apiKey, campaignId, userId);
    }
}
