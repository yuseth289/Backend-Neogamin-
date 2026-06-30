package com.neogaming.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neogaming.ai.analytics.dto.request.AnalyticsAIRequest;
import com.neogaming.ai.analytics.dto.response.AnalyticsResultDTO;
import com.neogaming.ai.search.dto.request.SearchAIRequest;
import com.neogaming.ai.search.dto.response.PythonSearchResultDTO;
import com.neogaming.ai.seller.dto.request.SellerOptimizeRequest;
import com.neogaming.ai.seller.dto.response.SellerAssistResultDTO;
import com.neogaming.ai.seller.dto.response.SellerBIDTO;
import com.neogaming.common.exception.AIServiceException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class AIServiceClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AIServiceProperties props;

    public AIServiceClient(ObjectMapper objectMapper, AIServiceProperties props) {
        this.objectMapper = objectMapper;
        this.props = props;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .build();
    }

    public PythonSearchResultDTO search(SearchAIRequest request) {
        String body = buildSearchJson(request.query(), request.clarification());
        String responseBody = post("/api/v1/ai/search", body);
        try {
            return objectMapper.readValue(responseBody, PythonSearchResultDTO.class);
        } catch (Exception e) {
            throw new AIServiceException("Failed to parse Python search response: " + e.getMessage());
        }
    }

    public AnalyticsResultDTO analytics(AnalyticsAIRequest request) {
        try {
            String body = objectMapper.writeValueAsString(request);
            String responseBody = post("/api/v1/ai/analytics", body);
            return objectMapper.readValue(responseBody, AnalyticsResultDTO.class);
        } catch (AIServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AIServiceException("Python AI analytics error: " + e.getMessage());
        }
    }

    public SellerAssistResultDTO sellerAssist(SellerOptimizeRequest request) {
        try {
            // Python expects nested: { productData: {...}, sellerId: "..." }
            java.util.Map<String, Object> productData = new java.util.LinkedHashMap<>();
            productData.put("name", request.name());
            if (request.category() != null)       productData.put("category", request.category());
            if (request.brand() != null)           productData.put("brand", request.brand());
            if (request.model() != null)           productData.put("model", request.model());
            if (request.priceCop() != null)        productData.put("priceCop", request.priceCop());
            if (request.rawDescription() != null)  productData.put("rawDescription", request.rawDescription());
            productData.put("features",      request.features()     != null ? request.features()     : java.util.List.of());
            productData.put("imagesBase64",  request.imagesBase64() != null ? request.imagesBase64() : java.util.List.of());

            java.util.Map<String, Object> nested = new java.util.LinkedHashMap<>();
            nested.put("productData", productData);
            nested.put("sellerId", request.sellerId() != null ? request.sellerId() : "anonymous");
            if (request.instruction() != null) nested.put("instruction", request.instruction());

            String body = objectMapper.writeValueAsString(nested);
            String responseBody = post("/api/v1/ai/seller/assist", body);
            return objectMapper.readValue(responseBody, SellerAssistResultDTO.class);
        } catch (AIServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AIServiceException("Python AI seller error: " + e.getMessage());
        }
    }

    public SellerBIDTO sellerBiQuery(String query, String sellerId) {
        try {
            String q = query.replace("\\", "\\\\").replace("\"", "\\\"");
            String s = sellerId.replace("\\", "\\\\").replace("\"", "\\\"");
            String body = "{\"query\":\"" + q + "\",\"sellerId\":\"" + s + "\"}";
            String responseBody = post("/api/v1/ai/seller/bi-query", body);
            return objectMapper.readValue(responseBody, SellerBIDTO.class);
        } catch (AIServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AIServiceException("Python AI seller BI error: " + e.getMessage());
        }
    }

    private String post(String path, String jsonBody) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(props.getBaseUrl() + path))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("X-Internal-Token", props.getInternalToken())
                    .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() >= 400) {
                throw new AIServiceException("Python AI error [" + response.statusCode() + "]: " + response.body());
            }
            return response.body();
        } catch (AIServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AIServiceException("Python AI unreachable: " + e.getMessage());
        }
    }

    private String buildSearchJson(String query, String clarification) {
        String q = query.replace("\\", "\\\\").replace("\"", "\\\"");
        if (clarification != null) {
            String c = clarification.replace("\\", "\\\\").replace("\"", "\\\"");
            return "{\"query\":\"" + q + "\",\"clarification\":\"" + c + "\"}";
        }
        return "{\"query\":\"" + q + "\"}";
    }
}
