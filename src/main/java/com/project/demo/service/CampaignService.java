package com.project.demo.service;

import com.project.demo.dto.CampaignRequest;
import com.project.demo.entity.Campaign;
import com.project.demo.enums.CampaignStatus;
import com.project.demo.repository.CampaignRepository;
import com.project.demo.repository.PrizeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CampaignService {
    private final CampaignRepository campaignRepository;
    private final PrizeRepository prizeRepository;

    @Transactional
    public Campaign createCampaign(CampaignRequest request) {
        Campaign campaign = Campaign.builder()
                .name(request.getName())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(CampaignStatus.ACTIVE)
                .build();
        return campaignRepository.save(campaign);
    }
}
