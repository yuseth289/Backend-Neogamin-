package com.neogaming.ai.analytics.controller;

import com.neogaming.ai.analytics.dto.request.AnalyticsAIRequest;
import com.neogaming.ai.analytics.dto.response.AnalyticsResultDTO;
import com.neogaming.ai.analytics.service.AnalyticsAIService;
import com.neogaming.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "AI Analytics", description = "Análisis de datos con IA para administradores")
@SecurityRequirement(name = "Bearer Authentication")
public class AnalyticsAIController {

    private final AnalyticsAIService analyticsAIService;

    @PostMapping("/query")
    @Operation(summary = "Consulta de analytics en lenguaje natural")
    public ResponseEntity<ApiResponse<AnalyticsResultDTO>> query(
            @RequestBody @Valid AnalyticsAIRequest request) {

        AnalyticsResultDTO result = analyticsAIService.query(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/report/{type}")
    @Operation(summary = "Reporte ejecutivo periódico")
    public ResponseEntity<ApiResponse<AnalyticsResultDTO>> generateReport(
            @PathVariable String type) {

        AnalyticsResultDTO result = analyticsAIService.generateReport(type);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
