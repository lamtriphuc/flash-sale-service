package com.project.demo.service;

import com.project.demo.dto.PrizeRequest;
import com.project.demo.entity.Campaign;
import com.project.demo.entity.Prize;
import com.project.demo.exception.NotFoundException;
import com.project.demo.repository.CampaignRepository;
import com.project.demo.repository.PrizeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PrizeService {
    private final PrizeRepository prizeRepository;
    private final CampaignRepository campaignRepository;

    @Transactional
    public Prize addPrize(PrizeRequest request) {
        Campaign campaign = campaignRepository.findById(request.getCampaignId())
                .orElseThrow(() -> new NotFoundException("Campaign not found"));

        Optional<Prize> existingPrize = prizeRepository.findByCampaignIdAndPrizeName(campaign.getId(), request.getPrizeName());

        if (existingPrize.isPresent()) {
            Prize prize = existingPrize.get();
            prize.setTotalQuantity(prize.getTotalQuantity() + request.getTotalQuantity());
            prize.setRemainQuantity(prize.getRemainQuantity() + request.getTotalQuantity());

            return prizeRepository.save(prize);
        }

        Prize prize = Prize.builder()
                .campaign(campaign)
                .prizeName(request.getPrizeName())
                .totalQuantity(request.getTotalQuantity())
                .remainQuantity(request.getTotalQuantity())
                .build();

        return prizeRepository.save(prize);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refundPrizeStock(Long prizeId) {
        prizeRepository.refundPrizeStock(prizeId);
    }
}
