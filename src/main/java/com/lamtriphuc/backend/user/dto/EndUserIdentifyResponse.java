package com.lamtriphuc.backend.user.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EndUserIdentifyResponse {
    private String userToken;
    private String idempotencyKey; // Khóa chống trùng lặp
}