package com.baas.flashsale.campaign.service;

import com.baas.flashsale.campaign.mapper.CampaignMapper;
import com.baas.flashsale.campaign.dto.CampaignResponse;
import com.baas.flashsale.campaign.dto.CreateCampaignRequest;
import com.baas.flashsale.campaign.entity.Campaign;
import com.baas.flashsale.campaign.entity.CampaignStatus;
import com.baas.flashsale.campaign.repository.CampaignRepository;
import com.baas.flashsale.common.BusinessException;
import com.baas.flashsale.security.CurrentTenant;
import com.baas.flashsale.flashsale.dto.CreateFlashSaleItemRequest;
import com.baas.flashsale.flashsale.dto.FlashSaleItemResponse;
import com.baas.flashsale.flashsale.mapper.FlashSaleItemMapper;
import com.baas.flashsale.flashsale.entity.FlashSaleItem;
import com.baas.flashsale.flashsale.repository.FlashSaleItemRepository;
import com.baas.flashsale.realtime.InventoryRealtimePublisher;
import com.baas.flashsale.tenant.entity.Tenant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CampaignService {
    private final CampaignRepository campaignRepository;
    private final FlashSaleItemRepository itemRepository;
    private final InventoryRealtimePublisher inventoryRealtimePublisher;
    private final CampaignMapper campaignMapper;
    private final FlashSaleItemMapper itemMapper;

    public CampaignResponse createCampaign(CreateCampaignRequest request) {
        Tenant tenant = CurrentTenant.get();
        validateCampaignTime(request.getStartTime(), request.getEndTime());
        String code = request.getCode().trim().toUpperCase();

        if (campaignRepository.existsByTenantIdAndCode(tenant.getId(), code)) {
            throw new BusinessException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Campaign code already exists in this tenant");
        }

        Campaign campaign = Campaign.builder()
                .tenant(tenant)
                .code(code)
                .name(request.getName().trim())
                .status(CampaignStatus.ACTIVE)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        return campaignMapper.toResponse(campaignRepository.save(campaign));
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaign(Long campaignId) {
        return campaignMapper.toResponse(findCampaignForTenant(campaignId, CurrentTenant.getId()));
    }

    @Transactional(readOnly = true)
    public List<FlashSaleItemResponse> getItems(Long campaignId) {
        findCampaignForTenant(campaignId, CurrentTenant.getId());
        return itemRepository.findByCampaignId(campaignId).stream()
                .map(itemMapper::toResponse)
                .toList();
    }

    public FlashSaleItemResponse addItem(Long campaignId, CreateFlashSaleItemRequest request) {
        Campaign campaign = findCampaignForTenant(campaignId, CurrentTenant.getId());

        if (request.getSalePrice() >= request.getOriginalPrice()) {
            throw new BusinessException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Sale price must be less than original price");
        }

        FlashSaleItem item = FlashSaleItem.builder()
                .campaign(campaign)
                .itemCode(request.getItemCode().trim())
                .itemName(request.getItemName().trim())
                .originalPrice(request.getOriginalPrice())
                .salePrice(request.getSalePrice())
                .totalQuantity(request.getTotalQuantity())
                .remainingQuantity(request.getTotalQuantity())
                .active(true)
                .build();

        FlashSaleItem savedItem = itemRepository.save(item);
        inventoryRealtimePublisher.publishAfterCommit(savedItem);
        return itemMapper.toResponse(savedItem);
    }

    private Campaign findCampaignForTenant(Long campaignId, Long tenantId) {
        return campaignRepository.findByIdAndTenantId(campaignId, tenantId)
                .orElseThrow(() -> new BusinessException("FORBIDDEN_RESOURCE", HttpStatus.FORBIDDEN, "Campaign does not belong to tenant"));
    }

    private void validateCampaignTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Start time must be before end time");
        }
    }

    public CampaignStatus resolveStatus(Campaign campaign) {
        return campaignMapper.resolveStatus(campaign);
    }
}
