package com.neogaming.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neogaming.ai.client.AIServiceClient;
import com.neogaming.ai.search.dto.request.SearchAIRequest;
import com.neogaming.ai.search.dto.response.PythonSearchResultDTO;
import com.neogaming.common.exception.AIServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SearchAIController — Unit con MockBean")
class SearchAIControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AIServiceClient aiServiceClient;

    // ── Happy path ─────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.co", roles = "CLIENT")
    @DisplayName("POST /api/v1/ai/search/intelligent → 200 con recomendaciones")
    void intelligentSearch_returnasRecomendaciones() throws Exception {
        PythonSearchResultDTO mockResult = new PythonSearchResultDTO(
                List.of(),
                java.util.Map.of(),
                false,
                null,
                "product_search",
                1500
        );
        when(aiServiceClient.search(any(SearchAIRequest.class))).thenReturn(mockResult);

        var body = new SearchAIRequest("mouse gamer FPS", null);

        mockMvc.perform(post("/api/v1/ai/search/intelligent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.intentClassified").value("product_search"))
                .andExpect(jsonPath("$.data.needsClarification").value(false));
    }

    // ── Error propagation ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.co", roles = "CLIENT")
    @DisplayName("Cuando Python AI falla → 502 Bad Gateway con errorCode AI_SERVICE_ERROR")
    void intelligentSearch_cuandoPythonFalla_devuelve502() throws Exception {
        when(aiServiceClient.search(any(SearchAIRequest.class)))
                .thenThrow(new AIServiceException("Python AI timeout"));

        var body = new SearchAIRequest("mouse gamer", null);

        mockMvc.perform(post("/api/v1/ai/search/intelligent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.errorCode").value("AI_SERVICE_ERROR"));
    }

    // ── Auth ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Sin JWT → 401 Unauthorized")
    void intelligentSearch_sinJwt_devuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/ai/search/intelligent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"mouse\"}"))
                .andExpect(status().isUnauthorized());
    }
}
