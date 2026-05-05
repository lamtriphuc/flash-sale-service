package com.project.demo.controller;

import com.project.demo.dto.PlayRequest;
import com.project.demo.security.CustomUserDetails;
import com.project.demo.service.PlayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plays")
@RequiredArgsConstructor
public class PlayController {
    private final PlayService playService;

    @PostMapping
    public ResponseEntity<String> playGame(
            @AuthenticationPrincipal CustomUserDetails customUserDetails, // Lấy ID từ token/header sau khi user Auth
            @RequestBody PlayRequest request
    ) {
        Long userId = customUserDetails.getUser().getId();
        // Lưu ý: Request này đã phải đi qua RateLimitInterceptor (Bucket4j) trước đó
        return ResponseEntity.ok(playService.play(userId, request));
    }
}
