package com.baas.flashsale.security;

import com.baas.flashsale.common.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    private static final String API_KEY_HEADER = "X-API-Key";

    private final ApiKeyService apiKeyService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !path.startsWith("/api/v1/campaigns");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String rawApiKey = request.getHeader(API_KEY_HEADER);
            ApiKeyContext context = apiKeyService.authenticate(rawApiKey);
            CurrentTenant.set(context.getTenant());
            filterChain.doFilter(request, response);
        } catch (BusinessException ex) {
            response.setStatus(ex.getStatus().value());
            response.setContentType("application/json");
            response.getWriter().write("""
                    {"code":"%s","message":"%s"}
                    """.formatted(ex.getCode(), ex.getMessage()));
        } finally {
            CurrentTenant.clear();
        }
    }
}
