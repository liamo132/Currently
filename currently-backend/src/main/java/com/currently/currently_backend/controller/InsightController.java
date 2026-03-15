package com.currently.currently_backend.controller;

import com.currently.currently_backend.dto.InsightGenerateRequest;
import com.currently.currently_backend.dto.InsightGenerateResponse;
import com.currently.currently_backend.service.InsightService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights")
public class InsightController {

    private final InsightService insightService;

    public InsightController(InsightService insightService) {
        this.insightService = insightService;
    }

    // this endpoint only works for logged-in users because security config protects it
    @PostMapping("/generate")
    public ResponseEntity<InsightGenerateResponse> generateInsights(@Valid @RequestBody InsightGenerateRequest request) {
        InsightGenerateResponse response = insightService.generateInsights(request);
        return ResponseEntity.ok(response);
    }

    // this endpoint returns the next unseen insight batch for an existing generation run
    @PostMapping("/{runId}/more")
    public ResponseEntity<InsightGenerateResponse> generateMore(@PathVariable String runId) {
        InsightGenerateResponse response = insightService.generateMore(runId);
        return ResponseEntity.ok(response);
    }
}
