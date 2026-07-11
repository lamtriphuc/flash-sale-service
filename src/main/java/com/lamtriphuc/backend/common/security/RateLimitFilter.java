package com.lamtriphuc.backend.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lamtriphuc.backend.common.dto.ApiResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    private final ProxyManager<byte[]> proxyManager;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Xác định Khóa định danh người gọi (Dựa vào User ID hoặc API Key hoặc IP)
        String callerId = resolveCallerId(request);

        // 2. Nếu không xác định được (chưa login, ko có key), tạm thời giới hạn bằng IP
        if (callerId == null) {
            callerId = "ip:" + request.getRemoteAddr();
        }

        // 3. Khởi tạo quy tắc cho cái Xô (Bucket): Cho phép 100 requests / 1 giây
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofSeconds(1))))
                .build();

        // 4. Lấy cái Xô của người dùng này từ Redis
        io.github.bucket4j.Bucket bucket = proxyManager.builder().build(callerId.getBytes(StandardCharsets.UTF_8), configuration);

        // 5. Thử lấy 1 token từ Xô
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Còn token -> Cho phép đi tiếp vào Controller
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            // Hết token -> Chặn lại ngay lập tức và ném lỗi 429 Too Many Requests
            log.warn("Rate limit exceeded for caller: {}", callerId);

            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.setHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ApiResponse<Void> apiResponse = ApiResponse.error(429, "Bạn đang thao tác quá nhanh. Vui lòng thử lại sau!");
            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
        }
    }

    private String resolveCallerId(HttpServletRequest request) {
        // Dựa vào API Key (Nếu là đối tác gọi Public API)
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isEmpty()) {
            return "apikey:" + apiKey;
        }

        // Dựa vào Token (Spring Security) - Nhưng Filter này chạy trước AuthFilter
        // Nên tốt nhất lấy thẳng Header Authorization để định danh (chưa cần giải mã xem đúng sai)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Băm chuỗi token ra làm ID (để tránh lưu nguyên token thô vào Redis key)
            return "token:" + authHeader.hashCode();
        }

        return null;
    }
}