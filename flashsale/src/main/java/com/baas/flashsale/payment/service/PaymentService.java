package com.baas.flashsale.payment.service;

import com.baas.flashsale.common.BusinessException;
import com.baas.flashsale.flashsale.dto.OrderResponse;
import com.baas.flashsale.flashsale.entity.Order;
import com.baas.flashsale.flashsale.entity.OrderStatus;
import com.baas.flashsale.flashsale.mapper.OrderMapper;
import com.baas.flashsale.flashsale.repository.OrderRepository;
import com.baas.flashsale.payment.dto.MockPaymentConfirmRequest;
import com.baas.flashsale.payment.dto.MockPaymentResult;
import com.baas.flashsale.payment.queue.PaymentEventMessage;
import com.baas.flashsale.payment.queue.PaymentEventProducer;
import com.baas.flashsale.payment.queue.PaymentEventType;
import com.baas.flashsale.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final PaymentEventProducer paymentEventProducer;

    @Transactional
    public OrderResponse mockConfirm(Long orderId, MockPaymentConfirmRequest request) {
        Order order = orderRepository.findWithLockById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND, "Order not found"));

        String currentUserId = AuthenticatedUser.get().getId().toString();
        if (!order.getParticipant().getExternalCustomerId().equals(currentUserId)) {
            throw new BusinessException("FORBIDDEN_RESOURCE", HttpStatus.FORBIDDEN, "Order does not belong to current user");
        }

        if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.CANCELLED) {
            return orderMapper.toResponse(order);
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException("INVALID_ORDER_STATE", HttpStatus.CONFLICT, "Order is not pending payment");
        }

        if (request.getResult() == MockPaymentResult.SUCCESS) {
            confirm(order);
        } else {
            cancel(order, PaymentEventType.PAYMENT_FAILED, "PAYMENT_FAILED");
        }

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public void expireOrder(Long orderId) {
        Order order = orderRepository.findWithLockById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return;
        }

        cancel(order, PaymentEventType.PAYMENT_TIMEOUT, "PAYMENT_TIMEOUT");
        orderRepository.save(order);
    }

    private void confirm(Order order) {
        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(LocalDateTime.now());
        paymentEventProducer.publishSuccess(toMessage(order, PaymentEventType.PAYMENT_SUCCESS));
    }

    private void cancel(Order order, PaymentEventType eventType, String failReason) {
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setFailReason(failReason);
        paymentEventProducer.publishFailed(toMessage(order, eventType));
    }

    private PaymentEventMessage toMessage(Order order, PaymentEventType eventType) {
        return new PaymentEventMessage(
                order.getId(),
                order.getCampaign().getId(),
                order.getItem().getId(),
                order.getParticipant().getExternalCustomerId(),
                eventType
        );
    }
}
