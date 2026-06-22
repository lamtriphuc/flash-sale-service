package com.baas.flashsale.auth.service;

import com.baas.flashsale.auth.dto.AuthResponse;
import com.baas.flashsale.auth.dto.LoginRequest;
import com.baas.flashsale.security.JwtService;
import com.baas.flashsale.security.TenantUserDetails;
import com.baas.flashsale.security.TenantUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TenantUserDetailsService userDetailsService;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        TenantUserDetails userDetails = loadUserDetails(request);

        if (!userDetails.isEnabled() || !passwordEncoder.matches(request.getPassword(), userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        return buildAuthResponse(userDetails);
    }

    private TenantUserDetails loadUserDetails(LoginRequest request) {
        String username = request.getUsername().trim();
        if (request.getTenantId() != null) {
            return userDetailsService.loadUserByTenantIdAndUsername(request.getTenantId(), username);
        }
        if (request.getTenantCode() != null && !request.getTenantCode().isBlank()) {
            return userDetailsService.loadUserByTenantCodeAndUsername(request.getTenantCode().trim().toUpperCase(), username);
        }
        throw new BadCredentialsException("Invalid username or password");
    }

    private AuthResponse buildAuthResponse(TenantUserDetails userDetails) {
        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(userDetails))
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
