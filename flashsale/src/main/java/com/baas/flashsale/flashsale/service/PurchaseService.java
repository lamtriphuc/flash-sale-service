package com.baas.flashsale.flashsale.service;

import com.baas.flashsale.campaign.service.CampaignService;
import com.baas.flashsale.campaign.entity.Campaign;
import com.baas.flashsale.campaign.entity.CampaignStatus;
import com.baas.flashsale.campaign.repository.CampaignRepository;
import com.baas.flashsale.common.BusinessException;
import com.baas.flashsale.security.CurrentTenant;
import com.baas.flashsale.flashsale.mapper.OrderMapper;
import com.baas.flashsale.flashsale.dto.OrderResponse;
import com.baas.flashsale.flashsale.dto.PurchaseRequest;
import com.baas.flashsale.flashsale.entity.FlashSaleItem;
import com.baas.flashsale.flashsale.entity.Order;
import com.baas.flashsale.flashsale.entity.OrderFailReason;
import com.baas.flashsale.flashsale.entity.OrderStatus;
import com.baas.flashsale.flashsale.repository.FlashSaleItemRepository;
import com.baas.flashsale.flashsale.repository.OrderRepository;
import com.baas.flashsale.participant.entity.Participant;
import com.baas.flashsale.participant.repository.ParticipantRepository;
import com.baas.flashsale.realtime.InventoryRealtimePublisher;
import com.baas.flashsale.tenant.entity.Tenant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseService {
    private final CampaignService campaignService;
    private final CampaignRepository campaignRepository;
    private final FlashSaleItemRepository itemRepository;
    private final ParticipantRepository participantRepository;
    private final OrderRepository orderRepository;
    private final InventoryRealtimePublisher inventoryRealtimePublisher;
    private final OrderMapper orderMapper;
    private final InventoryGateService inventoryGateService;

    @Transactional(noRollbackFor = BusinessException.class)
    public OrderResponse createOrder(Long campaignId, PurchaseRequest request) {
        Tenant tenant = CurrentTenant.get();
        Campaign campaign = findCampaignForTenant(campaignId, tenant.getId());
        String userId = request.getUserId().trim();
        FlashSaleItem item = itemRepository.findByIdAndCampaignId(request.getItemId(), campaignId)
                .orElseThrow(() -> new BusinessException("ITEM_NOT_FOUND", HttpStatus.NOT_FOUND, "Item not found in campaign"));

        CampaignStatus effectiveStatus = campaignService.resolveStatus(campaign);
        if (effectiveStatus != CampaignStatus.ACTIVE) {
            throw conflict(OrderFailReason.CAMPAIGN_NOT_ACTIVE, "Campaign is not active");
        }

        if (!Boolean.TRUE.equals(item.getActive())) {
            throw conflict(OrderFailReason.OUT_OF_STOCK, "Item is not active");
        }

        InventoryGateResult reservation = inventoryGateService.reserve(
                campaignId,
                item.getId(),
                userId,
                item.getRemainingQuantity()
        );

        if (reservation.status() == InventoryGateResult.Status.OUT_OF_STOCK) {
            throw conflict(OrderFailReason.OUT_OF_STOCK, "Item is out of stock");
        }

        if (reservation.status() == InventoryGateResult.Status.ALREADY_PURCHASED) {
            throw conflict(OrderFailReason.ALREADY_PURCHASED, "User already purchased in this campaign");
        }

        Participant participant = findOrCreateParticipant(tenant, userId);

        if (orderRepository.existsByCampaignIdAndParticipantIdAndStatus(campaignId, participant.getId(), OrderStatus.SUCCESS)) {
            inventoryGateService.releaseReservation(campaignId, item.getId(), userId);
            throw conflict(OrderFailReason.ALREADY_PURCHASED, "User already purchased in this campaign");
        }

        Order order = Order.builder()
                .tenant(tenant)
                .campaign(campaign)
                .item(item)
                .participant(participant)
                .status(OrderStatus.SUCCESS)
                .build();

        try {
            itemRepository.updateRemainingQuantity(item.getId(), reservation.remainingQuantity());
            item.setRemainingQuantity(reservation.remainingQuantity());

            Order savedOrder = orderRepository.save(order);
            inventoryRealtimePublisher.publishAfterCommit(item);
            return orderMapper.toResponse(savedOrder);
        } catch (RuntimeException ex) {
            inventoryGateService.releaseReservation(campaignId, item.getId(), userId);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(Long campaignId, String userId) {
        findCampaignForTenant(campaignId, CurrentTenant.getId());
        return orderRepository.findByCampaignIdAndParticipantExternalCustomerId(campaignId, userId)
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    private Campaign findCampaignForTenant(Long campaignId, Long tenantId) {
        return campaignRepository.findByIdAndTenantId(campaignId, tenantId)
                .orElseThrow(() -> new BusinessException("FORBIDDEN_RESOURCE", HttpStatus.FORBIDDEN, "Campaign does not belong to tenant"));
    }

    private Participant findOrCreateParticipant(Tenant tenant, String userId) {
        Long tenantId = tenant.getId();
        participantRepository.insertIfAbsent(tenantId, userId);
        return participantRepository.findByTenantIdAndExternalCustomerIdForUpdate(tenantId, userId)
                .orElseThrow(() -> new IllegalStateException("Participant was not created"));
    }

    private BusinessException conflict(OrderFailReason reason, String message) {
        return new BusinessException(reason.name(), HttpStatus.CONFLICT, message);
    }
}
