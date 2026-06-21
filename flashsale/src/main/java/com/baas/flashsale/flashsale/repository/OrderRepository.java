package com.baas.flashsale.flashsale.repository;

import com.baas.flashsale.flashsale.entity.Order;
import com.baas.flashsale.flashsale.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByCampaignIdAndParticipantIdAndStatus(Long campaignId, Long participantId, OrderStatus status);

    List<Order> findByCampaignIdAndParticipantExternalCustomerId(Long campaignId, String externalCustomerId);
}
