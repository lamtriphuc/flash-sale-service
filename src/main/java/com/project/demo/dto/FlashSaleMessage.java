package com.project.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlashSaleMessage {
    private Long campaignId;
    private Long userId;
    private Integer quantity;
}
