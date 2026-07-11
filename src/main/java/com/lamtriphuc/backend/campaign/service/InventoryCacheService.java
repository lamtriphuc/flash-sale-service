package com.lamtriphuc.backend.campaign.service;

import com.lamtriphuc.backend.campaign.entity.Campaign;
import com.lamtriphuc.backend.campaign.entity.Product;
import com.lamtriphuc.backend.campaign.repository.CampaignRepository;
import com.lamtriphuc.backend.campaign.repository.ProductRepository;
import com.lamtriphuc.backend.common.exception.AppException;
import com.lamtriphuc.backend.common.exception.ErrorCode;
import com.lamtriphuc.backend.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryCacheService {
    private final ProductRepository productRepository;
    private final CampaignRepository campaignRepository;
    private final StringRedisTemplate redisTemplate;

    public int syncCampaignInventoryToRedis(UUID campaignId) {
        UUID currentTenantId = SecurityUtils.getCurrentTenantId();

        Campaign campaign = campaignRepository.findByIdAndTenantId(campaignId, currentTenantId)
                .orElseThrow(() -> new AppException(ErrorCode.CAMPAIGN_NOT_FOUND));

        List<Product> products = productRepository.findByCampaignIdAndTenantId(campaignId, currentTenantId);

        if (products.isEmpty()) {
            log.warn("Chiến dịch {} không có sản phẩm nào để đồng bộ.", campaignId);
            return 0;
        }

        // Đổ dữ liệu lên Redis
        int syncCount = 0;
        for (Product product : products) {
            // Khóa Redis phải TUYỆT ĐỐI giống với khóa trong CheckoutService
            // Format: stock:{tenantId}:{productId}
            String stockKey = String.format("stock:%s:%s", currentTenantId, product.getId());

            // Lấy availableStock chứ KHÔNG PHẢI totalStock
            // (vì có thể có khách đã mua trước đó, hoặc admin chỉnh sửa tay)
            String stockValue = String.valueOf(product.getAvailableStock());

            // Ghi đè vào Redis (Nên dùng pipeline nếu số lượng sản phẩm lên tới hàng ngàn,
            // nhưng với Flash Sale vài chục sản phẩm thì vòng lặp for + SET là đủ nhanh).
            redisTemplate.opsForValue().set(stockKey, stockValue);

            // Xóa danh sách "những người đã giữ chỗ" của sản phẩm này (Reset lại từ đầu nếu admin muốn)
            // LƯU Ý: Bước này tùy thuộc vào business logic của bạn.
            // Nếu admin bấm đồng bộ lại giữa chừng lúc đang sale, không nên xóa các key user_bought.
            // Ở đây tôi giả định là đồng bộ trước giờ G, nên không xóa.

            syncCount++;
            log.info("Đồng bộ Redis -> Key: {}, Value: {}", stockKey, stockValue);
        }

        // 4. (Tùy chọn) Cập nhật trạng thái chiến dịch thành 'ACTIVE' nếu cần thiết
        // Nếu làm vậy, cần bỏ (readOnly = true) ở trên @Transactional

        return syncCount;
    }
}
