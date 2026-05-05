package com.project.demo.controller;

import com.project.demo.dto.CampaignRequest;
import com.project.demo.repository.CampaignRepository;
import com.project.demo.service.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {
    private final CampaignService campaignService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping()
    public ResponseEntity<?> createCampaign(@RequestBody CampaignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(campaignService.createCampaign(request));
    }
}
