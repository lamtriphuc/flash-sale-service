package com.project.demo.repository;

import com.project.demo.entity.Prize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrizeRepository extends JpaRepository<Prize, Long> {
    Optional<Prize> findByCampaignIdAndPrizeName(Long campaignId, String prizeName);

    @Modifying
    @Query("UPDATE Prize p SET p.remainQuantity = p.remainQuantity - 1 WHERE p.id = :prizeId AND p.remainQuantity > 0")
    int decrementPrizeStock(@Param("prizeId") Long prizeId);

    @Modifying
    @Query("UPDATE Prize p SET p.remainQuantity = p.remainQuantity + 1 WHERE p.id = :prizeId")
    int refundPrizeStock(@Param("prizeId") Long prizeId);
}
