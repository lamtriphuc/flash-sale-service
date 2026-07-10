package com.lamtriphuc.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EndUserIdentifyRequest {
    @NotBlank(message = "Thông tin liên hệ (Email/SĐT) không được để trống")
    private String identifier; // Khách hàng nhập email hoặc số điện thoại

    @NotBlank(message = "Mã thiết bị không hợp lệ")
    private String deviceFingerprint; // Mã do Frontend tự gen ra bằng thư viện FingerprintJS
}