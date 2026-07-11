package com.lamtriphuc.backend.common.security;

import com.lamtriphuc.backend.tenant.entity.ApiKey;
import com.lamtriphuc.backend.tenant.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {
    private final ApiKeyRepository apiKeyRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    )
            throws ServletException, IOException {
        // Chỉ xử lý các request đi vào public API hoặc checkout
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/v1/public/") && !uri.equals("/api/v1/orders/checkout")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String reqApiKey = request.getHeader("X-API-Key");

        if (reqApiKey == null || reqApiKey.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":401, \"message\":\"Thieu X-API-Key header\"}");
            return;
        }

        String hashedKey = hashApiKey(reqApiKey);

        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByApiKeyHashAndIsActiveTrue(hashedKey);

        if (apiKeyOpt.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":4010, \"message\":\"API Key khong hop le hoac da bi khoa\"}");
            return;
        }

        ApiKey apiKeyEntity = apiKeyOpt.get();

        // Đẩy TenantId vào SecurityContext (Tái sử dụng CustomUserDetails với role PUBLIC_CLIENT)
        CustomUserDetails userDetails = new CustomUserDetails(
                null, // Không có accountId vì đây là máy móc (Landing Page) gọi
                apiKeyEntity.getTenant().getId(),
                "public-client",
                "PUBLIC_CLIENT"
        );

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }

    private String hashApiKey(String rawApiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawApiKey.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Lỗi thuật toán Hash", e);
        }
    }
}
