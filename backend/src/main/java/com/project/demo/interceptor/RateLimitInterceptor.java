package com.project.demo.interceptor;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    // Lưu trữ bucket trong bộ nhớ (ConcurrentHashMap).
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(
           @NonNull HttpServletRequest request,
           @NonNull HttpServletResponse response,
           @NonNull Object handler
    ) throws Exception {
        if (!request.getRequestURI().startsWith("/api/plays")) {
            return true;
        }

        String clientIp = request.getRemoteAddr();

        Bucket bucket = cache.computeIfAbsent(clientIp, this::createNewBucket);

        if (bucket.tryConsume(1)) {
            return  true;
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Thao tác quá nhanh, vui lòng thử lại sau!");
            return false;
        }
    }

    private Bucket createNewBucket(String key) {
        // Cấu hình: Tối đa 2 request, hồi phục 1 request mỗi giây
        Bandwidth limit = Bandwidth.classic(2, Refill.greedy(1, Duration.ofSeconds(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
