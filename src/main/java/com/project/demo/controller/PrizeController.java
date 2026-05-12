package com.project.demo.controller;

import com.project.demo.dto.PrizeRequest;
import com.project.demo.service.PrizeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prizes")
@RequiredArgsConstructor
public class PrizeController {
    private final PrizeService prizeService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping()
    public ResponseEntity<?> addPrize(@RequestBody PrizeRequest request) {
        return ResponseEntity.ok(prizeService.addPrize(request));
    }
}
