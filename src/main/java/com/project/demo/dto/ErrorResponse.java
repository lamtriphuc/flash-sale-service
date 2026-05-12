package com.project.demo.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorResponse {
    private int status;           // Mã lỗi HTTP (VD: 400, 404, 500)
    private String error;         // Tên loại lỗi (VD: Bad Request)
    private String message;       // Lời nhắn chi tiết cho Client
    private LocalDateTime timestamp; // Thời gian xảy ra lỗi
}
