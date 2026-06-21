package com.baas.flashsale.auth;

import com.baas.flashsale.auth.dto.AuthResponse;
import com.baas.flashsale.security.JwtService;
import com.baas.flashsale.security.TenantUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
    private final JwtService jwtService;

    @GetMapping("/me")
    public AuthResponse me(@AuthenticationPrincipal TenantUserDetails userDetails) {
        return AuthResponse.builder()
                .tokenType("Bearer")
                .expiresInMs(jwtService.getExpirationMs())
                .userId(userDetails.getId())
                .tenantId(userDetails.getTenantId())
                .tenantCode(userDetails.getTenantCode())
                .username(userDetails.getUsername())
                .fullName(userDetails.getFullName())
                .role(userDetails.getRole())
                .build();
    }
}
