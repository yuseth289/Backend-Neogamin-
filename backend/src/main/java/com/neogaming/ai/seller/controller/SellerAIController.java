package com.neogaming.ai.seller.controller;

import com.neogaming.ai.seller.dto.request.SellerBIRequest;
import com.neogaming.ai.seller.dto.request.SellerOptimizeRequest;
import com.neogaming.ai.seller.dto.response.ImageEnhancementDTO;
import com.neogaming.ai.seller.dto.response.SellerAssistResultDTO;
import com.neogaming.ai.seller.dto.response.SellerBIDTO;
import com.neogaming.ai.seller.service.SellerAIService;
import com.neogaming.common.response.ApiResponse;
import com.neogaming.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/seller")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
@Tag(name = "AI Seller Assistant", description = "Asistente IA para vendedores")
@SecurityRequirement(name = "Bearer Authentication")
public class SellerAIController {

    private final SellerAIService sellerAIService;

    @PostMapping("/optimize")
    @Operation(summary = "Optimiza contenido del producto con IA")
    public ResponseEntity<ApiResponse<SellerAssistResultDTO>> optimize(
            @RequestBody @Valid SellerOptimizeRequest request) {

        String sellerId = SecurityUtils.getCurrentUserId().toString();
        SellerOptimizeRequest withSeller = new SellerOptimizeRequest(
                request.name(), request.category(), request.brand(), request.model(),
                request.priceCop(), request.rawDescription(), request.features(),
                request.imagesBase64(), sellerId, request.instruction());
        SellerAssistResultDTO result = sellerAIService.optimize(withSeller);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping(value = "/analyze-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Analiza calidad de imágenes del producto")
    public ResponseEntity<ApiResponse<SellerAssistResultDTO>> analyzeImages(
            @RequestParam String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "anonymous") String sellerId,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        SellerAssistResultDTO result = sellerAIService.analyzeImages(
                name, category, brand, sellerId, images == null ? List.of() : images);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/bi-query")
    @Operation(summary = "Consulta de inteligencia de negocio del vendedor")
    public ResponseEntity<ApiResponse<SellerBIDTO>> biQuery(
            @RequestBody @Valid SellerBIRequest request) {

        String sellerId = SecurityUtils.getCurrentUserId().toString();
        SellerBIDTO result = sellerAIService.biQuery(request.query(), sellerId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping(value = "/enhance-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Mejora imágenes del producto con Nano Banana")
    public ResponseEntity<ApiResponse<ImageEnhancementDTO>> enhanceImages(
            @RequestPart("images") List<MultipartFile> images,
            @RequestParam(required = false) List<String> operations,
            @RequestParam(required = false) String productName,
            @RequestParam(defaultValue = "false") boolean generatePromotional) {

        List<String> ops = operations != null && !operations.isEmpty()
                ? operations
                : List.of("background_removal", "white_background", "color_correction", "sharpening");

        ImageEnhancementDTO result = sellerAIService.enhanceImages(images, ops, productName, generatePromotional);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
