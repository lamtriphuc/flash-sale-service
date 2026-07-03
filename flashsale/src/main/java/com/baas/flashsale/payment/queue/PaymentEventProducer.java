package com.baas.flashsale.payment.queue;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class PaymentEventProducer {
    private final RabbitTemplate rabbitTemplate;

    public void publishSuccess(PaymentEventMessage message) {
        publish(PaymentQueueConfig.SUCCESS_ROUTING_KEY, message);
    }

    public void publishFailed(PaymentEventMessage message) {
        publish(PaymentQueueConfig.FAILED_ROUTING_KEY, message);
    }

    private void publish(String routingKey, PaymentEventMessage message) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            rabbitTemplate.convertAndSend(PaymentQueueConfig.EXCHANGE, routingKey, message);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rabbitTemplate.convertAndSend(PaymentQueueConfig.EXCHANGE, routingKey, message);
            }
        });
    }
}
