package com.baas.flashsale.realtime;

import com.baas.flashsale.flashsale.entity.FlashSaleItem;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class InventoryRealtimePublisher {
    private final SimpMessagingTemplate messagingTemplate;

    public void publishAfterCommit(FlashSaleItem item) {
        InventoryUpdateMessage message = InventoryUpdateMessage.builder()
                .campaignId(item.getCampaign().getId())
                .itemId(item.getId())
                .itemCode(item.getItemCode())
                .remainingQuantity(item.getRemainingQuantity())
                .updatedAt(LocalDateTime.now())
                .build();

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish(message);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish(message);
            }
        });
    }

    private void publish(InventoryUpdateMessage message) {
        String destination = "/topic/campaigns/%d/items/%d/inventory"
                .formatted(message.getCampaignId(), message.getItemId());
        messagingTemplate.convertAndSend(destination, message);
    }
}
