package com.baas.flashsale.campaign;

import com.baas.flashsale.campaign.dto.CampaignResponse;
import com.baas.flashsale.campaign.dto.CreateCampaignRequest;
import com.baas.flashsale.campaign.entity.Campaign;
import com.baas.flashsale.campaign.entity.CampaignStatus;
import com.baas.flashsale.campaign.repository.CampaignRepository;
import com.baas.flashsale.common.BusinessException;
import com.baas.flashsale.flashsale.dto.CreateFlashSaleItemRequest;
import com.baas.flashsale.flashsale.dto.FlashSaleItemResponse;
import com.baas.flashsale.flashsale.entity.FlashSaleItem;
import com.baas.flashsale.flashsale.repository.FlashSaleItemRepository;
import com.baas.flashsale.security.ApiKeyContext;
import com.baas.flashsale.security.ApiKeyService;
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
    private final ApiKeyService apiKeyService;

    public CampaignResponse createCampaign(String rawApiKey, CreateCampaignRequest request) {
        ApiKeyContext context = apiKeyService.authenticate(rawApiKey);

        validateCampaignTime(request.getStartTime(), request.getEndTime());
        String code = request.getCode().trim().toUpperCase();

        if (campaignRepository.existsByTenantIdAndCode(context.getTenant().getId(), code)) {
            throw new BusinessException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Campaign code already exists in this tenant");
        }

        Campaign campaign = Campaign.builder()
                .tenant(context.getTenant())
                .code(code)
                .name(request.getName().trim())
                .status(CampaignStatus.ACTIVE)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        return mapCampaign(campaignRepository.save(campaign));
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaign(String rawApiKey, Long campaignId) {
        ApiKeyContext context = apiKeyService.authenticate(rawApiKey);
        return mapCampaign(findCampaignForTenant(campaignId, context.getTenant().getId()));
    }

    @Transactional(readOnly = true)
    public List<FlashSaleItemResponse> getItems(String rawApiKey, Long campaignId) {
        ApiKeyContext context = apiKeyService.authenticate(rawApiKey);
        findCampaignForTenant(campaignId, context.getTenant().getId());
        return itemRepository.findByCampaignId(campaignId).stream()
                .map(this::mapItem)
                .toList();
    }

    public FlashSaleItemResponse addItem(String rawApiKey, Long campaignId, CreateFlashSaleItemRequest request) {
        ApiKeyContext context = apiKeyService.authenticate(rawApiKey);
        Campaign campaign = findCampaignForTenant(campaignId, context.getTenant().getId());

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

        return mapItem(itemRepository.save(item));
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
        if (campaign.getStatus() == CampaignStatus.CANCELLED) {
            return CampaignStatus.CANCELLED;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(campaign.getStartTime())) {
            return CampaignStatus.DRAFT;
        }
        if (now.isAfter(campaign.getEndTime())) {
            return CampaignStatus.ENDED;
        }
        return CampaignStatus.ACTIVE;
    }

    private CampaignResponse mapCampaign(Campaign campaign) {
        return CampaignResponse.builder()
                .id(campaign.getId())
                .tenantId(campaign.getTenant().getId())
                .code(campaign.getCode())
                .name(campaign.getName())
                .status(resolveStatus(campaign))
                .startTime(campaign.getStartTime())
                .endTime(campaign.getEndTime())
                .createdAt(campaign.getCreatedAt())
                .build();
    }

    private FlashSaleItemResponse mapItem(FlashSaleItem item) {
        return FlashSaleItemResponse.builder()
                .id(item.getId())
                .campaignId(item.getCampaign().getId())
                .itemCode(item.getItemCode())
                .itemName(item.getItemName())
                .originalPrice(item.getOriginalPrice())
                .salePrice(item.getSalePrice())
                .totalQuantity(item.getTotalQuantity())
                .remainingQuantity(item.getRemainingQuantity())
                .active(item.getActive())
                .build();
    }
}
