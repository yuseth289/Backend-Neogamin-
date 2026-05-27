package com.neogaming.chat.controller;

import com.neogaming.chat.dto.request.SendMessageRequest;
import com.neogaming.chat.dto.request.StartConversationRequest;
import com.neogaming.chat.dto.response.ConversationResponse;
import com.neogaming.chat.dto.response.MessageResponse;
import com.neogaming.chat.service.ChatService;
import com.neogaming.common.response.ApiResponse;
import com.neogaming.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chat", description = "Mensajería entre compradores y vendedores")
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok(
                chatService.listConversations(SecurityUtils.getCurrentUserId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ConversationResponse>> iniciar(
            @Valid @RequestBody StartConversationRequest req) {
        ConversationResponse conv = chatService.startConversation(req, SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(conv));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConversationResponse>> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                chatService.getConversation(id, SecurityUtils.getCurrentUserId())));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> mensajes(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                chatService.getMessages(id, SecurityUtils.getCurrentUserId())));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> enviar(
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest req) {
        MessageResponse msg = chatService.sendMessage(id, req, SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(msg));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable UUID id) {
        chatService.deleteConversation(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> marcarLeido(@PathVariable UUID id) {
        chatService.markRead(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
