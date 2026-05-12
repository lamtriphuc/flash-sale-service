package com.project.demo.dto;

import lombok.Data;

@Data
public class PrizeRequest {
    private Long campaignId;
    private String prizeName;
    private Integer totalQuantity;
}
