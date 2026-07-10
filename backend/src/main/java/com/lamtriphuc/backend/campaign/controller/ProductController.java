package com.lamtriphuc.backend.campaign.controller;

import com.lamtriphuc.backend.campaign.dto.ProductAddRequest;
import com.lamtriphuc.backend.campaign.dto.ProductResponse;
import com.lamtriphuc.backend.campaign.service.ProductService;
import com.lamtriphuc.backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping("/campaigns/{campaignId}/products")
    public ResponseEntity<ApiResponse<ProductResponse>> addProduct(
            @PathVariable UUID campaignId,
            @Valid @RequestBody ProductAddRequest request
    ) {

        ProductResponse response = productService.addProductToCampaign(campaignId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/campaigns/{campaignId}/products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsByCampaign(
            @PathVariable UUID campaignId) {

        List<ProductResponse> responses = productService.getProductsByCampaign(campaignId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
