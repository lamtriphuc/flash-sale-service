package com.project.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CampaignRequest {
    private String name;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
