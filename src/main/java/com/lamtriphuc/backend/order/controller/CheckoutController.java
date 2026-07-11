package com.lamtriphuc.backend.order.controller;

import com.lamtriphuc.backend.common.dto.ApiResponse;
import com.lamtriphuc.backend.order.dto.CheckoutRequest;
import com.lamtriphuc.backend.order.service.CheckoutService;
import com.lamtriphuc.backend.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class CheckoutController {
    private final CheckoutService checkoutService;
    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<Void>> checkout(@Valid @RequestBody CheckoutRequest request) {
        checkoutService.processCheckout(request);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(null));
    }

    // Mock: Nút "Thanh toán thành công"
    @PostMapping("/{orderId}/success")
    public ResponseEntity<ApiResponse<String>> mockPaymentSuccess(@PathVariable UUID orderId) {
        // Gọi Service xử lý chốt đơn (Đổi status -> PAID, tắt báo thức Redis)
        orderService.confirmPaymentSuccess(orderId);

        return ResponseEntity.ok(ApiResponse.success("Thanh toán giả lập thành công!"));
    }

    // Mock: Nút "Hủy thanh toán"
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<String>> mockPaymentCancel(@PathVariable UUID orderId) {
        // Gọi hàm hủy đơn (Đổi status -> CANCELLED, hoàn lại kho Redis)
        orderService.cancelExpiredOrder(orderId);

        return ResponseEntity.ok(ApiResponse.success("Đã hủy đơn và hoàn kho giả lập!"));
    }
}
