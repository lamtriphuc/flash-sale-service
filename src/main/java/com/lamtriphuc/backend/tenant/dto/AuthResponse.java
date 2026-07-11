package com.lamtriphuc.backend.tenant.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthResponse {
    private String accessToken;
    private String companyName;
    private String role;
    private String publicKey;
    private String secretKey;
}
