package com.baas.flashsale.flashsale.repository;

import com.baas.flashsale.flashsale.entity.FlashSaleItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FlashSaleItemRepository extends JpaRepository<FlashSaleItem, Long> {
    List<FlashSaleItem> findByCampaignId(Long campaignId);

    Optional<FlashSaleItem> findByIdAndCampaignId(Long id, Long campaignId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from FlashSaleItem i where i.id = :id and i.campaign.id = :campaignId")
    Optional<FlashSaleItem> findByIdAndCampaignIdForUpdate(@Param("id") Long id, @Param("campaignId") Long campaignId);

    @Modifying
    @Query("update FlashSaleItem i set i.remainingQuantity = :remainingQuantity where i.id = :id")
    void updateRemainingQuantity(@Param("id") Long id, @Param("remainingQuantity") Integer remainingQuantity);

    @Modifying
    @Query("update FlashSaleItem i set i.remainingQuantity = i.remainingQuantity + 1 where i.id = :id")
    void incrementRemainingQuantity(@Param("id") Long id);
}
