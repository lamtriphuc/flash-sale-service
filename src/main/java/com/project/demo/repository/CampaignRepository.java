package com.project.demo.repository;

import com.project.demo.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    @Query("SELECT c FROM Campaign c WHERE c.status = 'ACTIVE' AND c.startTime <= CURRENT_TIMESTAMP AND c.endTime >= CURRENT_TIMESTAMP")
    List<Campaign> findActiveCampaigns();
}