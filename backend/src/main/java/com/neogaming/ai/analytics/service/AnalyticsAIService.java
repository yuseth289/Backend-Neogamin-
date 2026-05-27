package com.neogaming.ai.analytics.service;

import com.neogaming.ai.client.AIServiceClient;
import com.neogaming.ai.analytics.dto.request.AnalyticsAIRequest;
import com.neogaming.ai.analytics.dto.response.AnalyticsResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsAIService {

    private final AIServiceClient aiServiceClient;

    public AnalyticsResultDTO query(AnalyticsAIRequest request) {
        return aiServiceClient.analytics(request);
    }

    public AnalyticsResultDTO generateReport(String reportType) {
        AnalyticsAIRequest request = new AnalyticsAIRequest(
                "Genera un reporte completo de " + reportType,
                null, null, reportType
        );
        return aiServiceClient.analytics(request);
    }
}
