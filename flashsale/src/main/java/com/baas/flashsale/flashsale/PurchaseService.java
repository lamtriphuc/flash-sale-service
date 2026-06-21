package com.baas.flashsale.flashsale;

import com.baas.flashsale.campaign.CampaignService;
import com.baas.flashsale.campaign.entity.Campaign;
import com.baas.flashsale.campaign.entity.CampaignStatus;
import com.baas.flashsale.campaign.repository.CampaignRepository;
import com.baas.flashsale.common.BusinessException;
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
import com.baas.flashsale.security.ApiKeyContext;
import com.baas.flashsale.security.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseService {
    private final ApiKeyService apiKeyService;
    private final CampaignService campaignService;
    private final CampaignRepository campaignRepository;
    private final FlashSaleItemRepository itemRepository;
    private final ParticipantRepository participantRepository;
    private final OrderRepository orderRepository;

    @Transactional(noRollbackFor = BusinessException.class)
    public OrderResponse purchase(String rawApiKey, Long campaignId, Long itemId, PurchaseRequest request) {
        ApiKeyContext context = apiKeyService.authenticate(rawApiKey);
        Campaign campaign = findCampaignForTenant(campaignId, context.getTenant().getId());
        Participant participant = findOrCreateParticipant(context, request.getUserId().trim());
        FlashSaleItem item = itemRepository.findByIdAndCampaignIdForUpdate(itemId, campaignId)
                .orElseThrow(() -> new BusinessException("ITEM_NOT_FOUND", HttpStatus.NOT_FOUND, "Item not found in campaign"));

        CampaignStatus effectiveStatus = campaignService.resolveStatus(campaign);
        if (effectiveStatus != CampaignStatus.ACTIVE) {
            Order order = saveFailedOrder(context, campaign, item, participant, OrderFailReason.CAMPAIGN_NOT_ACTIVE);
            throw conflict(OrderFailReason.CAMPAIGN_NOT_ACTIVE, "Campaign is not active", order);
        }

        if (!Boolean.TRUE.equals(item.getActive()) || item.getRemainingQuantity() <= 0) {
            Order order = saveFailedOrder(context, campaign, item, participant, OrderFailReason.OUT_OF_STOCK);
            throw conflict(OrderFailReason.OUT_OF_STOCK, "Item is out of stock", order);
        }

        if (orderRepository.existsByCampaignIdAndParticipantIdAndStatus(campaignId, participant.getId(), OrderStatus.SUCCESS)) {
            Order order = saveFailedOrder(context, campaign, item, participant, OrderFailReason.ALREADY_PURCHASED);
            throw conflict(OrderFailReason.ALREADY_PURCHASED, "User already purchased in this campaign", order);
        }

        item.setRemainingQuantity(item.getRemainingQuantity() - 1);
        itemRepository.save(item);

        Order order = Order.builder()
                .tenant(context.getTenant())
                .campaign(campaign)
                .item(item)
                .participant(participant)
                .status(OrderStatus.SUCCESS)
                .build();

        return mapOrder(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(String rawApiKey, Long campaignId, String userId) {
        ApiKeyContext context = apiKeyService.authenticate(rawApiKey);
        findCampaignForTenant(campaignId, context.getTenant().getId());
        return orderRepository.findByCampaignIdAndParticipantExternalCustomerId(campaignId, userId)
                .stream()
                .map(this::mapOrder)
                .toList();
    }

    private Campaign findCampaignForTenant(Long campaignId, Long tenantId) {
        return campaignRepository.findByIdAndTenantId(campaignId, tenantId)
                .orElseThrow(() -> new BusinessException("FORBIDDEN_RESOURCE", HttpStatus.FORBIDDEN, "Campaign does not belong to tenant"));
    }

    private Participant findOrCreateParticipant(ApiKeyContext context, String userId) {
        return participantRepository.findByTenantIdAndExternalCustomerId(context.getTenant().getId(), userId)
                .orElseGet(() -> participantRepository.save(Participant.builder()
                        .tenant(context.getTenant())
                        .externalCustomerId(userId)
                        .build()));
    }

    private Order saveFailedOrder(
            ApiKeyContext context,
            Campaign campaign,
            FlashSaleItem item,
            Participant participant,
            OrderFailReason reason
    ) {
        Order order = Order.builder()
                .tenant(context.getTenant())
                .campaign(campaign)
                .item(item)
                .participant(participant)
                .status(OrderStatus.FAILED)
                .failReason(reason.name())
                .build();
        return orderRepository.save(order);
    }

    private BusinessException conflict(OrderFailReason reason, String message, Order order) {
        return new BusinessException(reason.name(), HttpStatus.CONFLICT, message + " (orderId=" + order.getId() + ")");
    }

    private OrderResponse mapOrder(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .tenantId(order.getTenant().getId())
                .campaignId(order.getCampaign().getId())
                .itemId(order.getItem().getId())
                .itemCode(order.getItem().getItemCode())
                .userId(order.getParticipant().getExternalCustomerId())
                .status(order.getStatus())
                .failReason(order.getFailReason())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
