package com.baas.flashsale.payment.service;

import com.baas.flashsale.flashsale.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentNotificationService {
    private final JavaMailSender mailSender;

    public void sendPaymentSuccess(Order order) {
        send(order, "FlashDeal order confirmed", "Your FlashDeal order #" + order.getId() + " has been confirmed.");
    }

    public void sendPaymentFailed(Order order) {
        send(order, "FlashDeal payment failed", "Your FlashDeal order #" + order.getId() + " was cancelled and stock was returned.");
    }

    private void send(Order order, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("user-" + order.getParticipant().getExternalCustomerId() + "@flashdeal.local");
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Sent payment email for order {}", order.getId());
        } catch (RuntimeException ex) {
            log.warn("Payment email could not be sent for order {}", order.getId(), ex);
        }
    }
}
