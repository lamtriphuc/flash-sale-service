package com.baas.flashsale.auth.service;

import com.baas.flashsale.auth.dto.AuthResponse;
import com.baas.flashsale.auth.dto.LoginRequest;
import com.baas.flashsale.auth.dto.RefreshTokenRequest;
import com.baas.flashsale.auth.dto.RegisterRequest;
import com.baas.flashsale.security.JwtService;
import com.baas.flashsale.security.TenantUserDetails;
import com.baas.flashsale.security.TenantUserDetailsService;
import com.baas.flashsale.tenant.entity.Tenant;
import com.baas.flashsale.tenant.entity.User;
import com.baas.flashsale.tenant.entity.UserRole;
import com.baas.flashsale.tenant.repository.TenantRepository;
import com.baas.flashsale.tenant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private static final String DEFAULT_TENANT_CODE = "FLASHDEAL";

    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TenantUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;

    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BadCredentialsException("Email already exists");
        }

        Tenant tenant = getOrCreateDefaultTenant();
        User user = User.builder()
                .tenant(tenant)
                .username(email)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(UserRole.USER)
                .active(true)
                .build();

        return buildAuthResponse(new TenantUserDetails(userRepository.save(user)));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        TenantUserDetails userDetails = userDetailsService.loadUserByEmail(request.getEmail().trim().toLowerCase());

        if (!userDetails.isEnabled() || !passwordEncoder.matches(request.getPassword(), userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        return buildAuthResponse(userDetails);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken().trim();
        String email = jwtService.extractUsername(refreshToken);
        TenantUserDetails userDetails = userDetailsService.loadUserByEmail(email);

        if (!jwtService.isRefreshTokenValid(refreshToken, userDetails)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        return buildAuthResponse(userDetails);
    }

    private AuthResponse buildAuthResponse(TenantUserDetails userDetails) {
        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(userDetails))
                .refreshToken(jwtService.generateRefreshToken(userDetails))
                .tokenType("Bearer")
                .expiresInMs(jwtService.getExpirationMs())
                .refreshExpiresInMs(jwtService.getRefreshExpirationMs())
                .userId(userDetails.getId())
                .tenantId(userDetails.getTenantId())
                .tenantCode(userDetails.getTenantCode())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .fullName(userDetails.getFullName())
                .role(userDetails.getRole())
                .build();
    }

    private Tenant getOrCreateDefaultTenant() {
        return tenantRepository.findByCode(DEFAULT_TENANT_CODE)
                .orElseGet(() -> tenantRepository.save(Tenant.builder()
                        .code(DEFAULT_TENANT_CODE)
                        .name("FlashDeal")
                        .active(true)
                        .build()));
    }
}
