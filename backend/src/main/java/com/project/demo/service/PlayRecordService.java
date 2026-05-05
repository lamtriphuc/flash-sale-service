package com.project.demo.service;

import com.project.demo.dto.PlayMessage;
import com.project.demo.entity.PlayRecord;
import com.project.demo.enums.RecordStatus;
import com.project.demo.repository.PlayRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PlayRecordService {
    private final PlayRecordRepository recordRepository;

    // Bắt buộc dùng REQUIRES_NEW
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveWinningRecord(PlayMessage message) {
        PlayRecord record = PlayRecord.builder()
                .userId(message.getUserId())
                .campaignId(message.getCampaignId())
                .prizeId(message.getPrizeId())
                .idempotencyKey(message.getIdempotencyKey())
                .status(RecordStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        // BẮT BUỘC DÙNG saveAndFlush() ĐỂ ÉP BẮN RA LỖI NGAY LẬP TỨC NẾU TRÙNG
        recordRepository.saveAndFlush(record);
    }
}