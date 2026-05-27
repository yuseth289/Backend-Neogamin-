package com.neogaming.ai.search.controller;

import com.neogaming.ai.search.dto.request.SearchAIRequest;
import com.neogaming.ai.search.dto.response.SearchResultDTO;
import com.neogaming.ai.search.service.SearchAIService;
import com.neogaming.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/ai/search")
@RequiredArgsConstructor
@Tag(name = "AI Search", description = "Búsqueda inteligente con lenguaje natural")
@SecurityRequirement(name = "Bearer Authentication")
public class SearchAIController {

    private final SearchAIService searchAIService;

    @PostMapping("/intelligent")
    @Operation(summary = "Búsqueda inteligente con NLP")
    public ResponseEntity<ApiResponse<SearchResultDTO>> intelligentSearch(
            @RequestBody @Valid SearchAIRequest request,
            Principal principal) {

        SearchResultDTO result = searchAIService.intelligentSearch(request, principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping(value = "/intelligent/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Búsqueda inteligente con SSE streaming")
    public SseEmitter intelligentSearchStream(
            @RequestParam String query,
            @RequestParam(required = false) String clarification,
            Principal principal) {

        SseEmitter emitter = new SseEmitter(45_000L);

        CompletableFuture.runAsync(() -> {
            try {
                SearchResultDTO result = searchAIService.intelligentSearch(
                        new SearchAIRequest(query, clarification),
                        principal.getName()
                );
                emitter.send(SseEmitter.event()
                        .name("result")
                        .data(result, MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
