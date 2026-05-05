package com.project.demo.service;

import com.project.demo.dto.PlayMessage;
import com.project.demo.dto.PlayRequest;
import com.project.demo.dto.PrizeRequest;
import com.project.demo.entity.Campaign;
import com.project.demo.entity.Prize;
import com.project.demo.repository.CampaignRepository;
import com.project.demo.repository.PrizeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayService {
    private final PrizeRepository prizeRepository;
    private final RabbitTemplate rabbitTemplate;


    @Transactional
    public String play(Long userId, PlayRequest request) {
        // 1. (Optional) Check chiến dịch còn Active không
        // Logic kiểm tra thời gian hợp lệ ở đây...

        // 2. CHỐNG RACE CONDITION TRỰC TIẾP TRÊN DB
        // Query sẽ lock dòng này và trừ 1. Nếu hết (hoặc sai ID), trả về 0.
        int updatedRows = prizeRepository.decrementPrizeStock(request.getPrizeId());

        if (updatedRows == 1) {
            // 3. TRÚNG THƯỞNG: Tạo Message đẩy vào RabbitMQ
            PlayMessage message = PlayMessage.builder()
                    .userId(userId)
                    .campaignId(request.getCampaignId())
                    .prizeId(request.getPrizeId())
                    .idempotencyKey(request.getIdempotencyKey())
                    .build();

            rabbitTemplate.convertAndSend("campaign.exchange", "routing.key.success", message);

            return "Chúc mừng bạn đã trúng thưởng!";
        }

        return "Rất tiếc, phần quà đã hết hoặc bạn không may mắn!";
    }
}
