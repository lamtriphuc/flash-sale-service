package com.baas.flashsale.payment.service;

import com.baas.flashsale.flashsale.entity.Order;
import com.baas.flashsale.flashsale.entity.OrderStatus;
import com.baas.flashsale.flashsale.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PaymentTimeoutScheduler {
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

    @Scheduled(fixedDelayString = "${app.payment.timeout-scan-ms:60000}")
    public void cancelExpiredPendingPayments() {
        orderRepository.findByStatusAndPaymentExpiresAtBefore(OrderStatus.PENDING_PAYMENT, LocalDateTime.now())
                .stream()
                .map(Order::getId)
                .forEach(paymentService::expireOrder);
    }
}
