package com.project.demo.repository;

import com.project.demo.entity.PlayRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayRecordRepository extends JpaRepository<PlayRecord, Long> {
    boolean existsByUserIdAndCampaignId(Long userId, Long campaignId);
    Optional<PlayRecord> findByIdempotencyKey(String idempotencyKey);
}
