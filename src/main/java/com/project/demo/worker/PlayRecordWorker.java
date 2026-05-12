package com.project.demo.worker;

import com.project.demo.dto.PlayMessage;
import com.project.demo.entity.PlayRecord;
import com.project.demo.enums.RecordStatus;
import com.project.demo.repository.PlayRecordRepository;
import com.project.demo.repository.PrizeRepository;
import com.project.demo.service.PlayRecordService;
import com.project.demo.service.PrizeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlayRecordWorker {
    private final PlayRecordService playRecordService;
    private final PrizeService prizeService;

    @RabbitListener(queues = "campaign.queue.success")
    public void processWinningRecord(PlayMessage message) {
        try {
            playRecordService.saveWinningRecord(message);
            log.info("Đã lưu kết quả trúng thưởng cho User: {}", message.getUserId());

        } catch (DataIntegrityViolationException e) {
            // Catch được lỗi do DB báo trùng IdempotencyKey (User lag mạng gửi đúp message)
            log.warn("Bỏ qua message trùng lặp (IdempotencyKey): {}", message.getIdempotencyKey());
            prizeService.refundPrizeStock(message.getPrizeId());
        } catch (Exception e) {
            // Lỗi khác: Có thể đẩy vào Dead Letter Queue (DLQ) để xử lý sau
            log.error("Lỗi khi lưu kết quả: ", e);
        }
    }
}
