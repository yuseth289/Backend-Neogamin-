package com.neogaming.ai.search.service;

import com.neogaming.ai.client.AIServiceClient;
import com.neogaming.ai.search.dto.request.SearchAIRequest;
import com.neogaming.ai.search.dto.response.PythonSearchResultDTO;
import com.neogaming.ai.search.dto.response.SearchResultDTO;
import com.neogaming.catalog.product.domain.Product;
import com.neogaming.catalog.product.repository.ProductImageRepository;
import com.neogaming.catalog.product.repository.ProductRepository;
import com.neogaming.common.enums.EstadoProducto;
import com.neogaming.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchAIService {

    private final AIServiceClient aiServiceClient;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductImageRepository productImageRepository;

    private static final NumberFormat COP_FORMAT = NumberFormat.getInstance(new Locale("es", "CO"));

    public SearchResultDTO intelligentSearch(SearchAIRequest request, String userId) {
        // 1. Build request for Python (add userId/sessionId)
        var pythonRequest = new com.neogaming.ai.search.dto.request.SearchAIRequest(
                request.query(), request.clarification()
        );

        // 2. Call Python AI agent (synchronous, RestClient)
        PythonSearchResultDTO pythonResult = aiServiceClient.search(pythonRequest);

        // 3. Enrich recommendations with live DB data
        List<SearchResultDTO.ProductRecommendationDTO> enriched = new ArrayList<>();
        for (PythonSearchResultDTO.RecommendationDTO rec : pythonResult.recommendations()) {
            try {
                UUID productId = UUID.fromString(rec.productId());
                Optional<Product> productOpt = productRepository.findById(productId);
                if (productOpt.isEmpty() || productOpt.get().getStatus() != EstadoProducto.ACTIVE) {
                    continue;
                }
                Product p = productOpt.get();
                boolean stockAvailable = inventoryRepository.findByProductId(p.getId())
                        .map(inv -> inv.getAvailableStock() > 0)
                        .orElse(false);
                if (!stockAvailable) {
                    continue;
                }
                String primaryImageUrl = productImageRepository
                        .findByProductIdAndPrimaryTrue(p.getId())
                        .map(img -> img.getUrl())
                        .orElse(null);

                enriched.add(new SearchResultDTO.ProductRecommendationDTO(
                        rec.productId(),
                        p.getSlug(),
                        p.getName(),
                        p.getBasePrice() != null ? p.getBasePrice().longValue() : null,
                        p.getBasePrice() != null ? "$ " + COP_FORMAT.format(p.getBasePrice().longValue()) : null,
                        rec.relevanceScore(),
                        rec.explanation(),
                        rec.compatibilityNotes(),
                        rec.priceFit(),
                        primaryImageUrl,
                        stockAvailable
                ));
            } catch (IllegalArgumentException ignored) {
                // Invalid UUID from Python — skip
            }
        }

        return new SearchResultDTO(
                pythonResult.greeting(),
                pythonResult.closingMessage(),
                enriched,
                pythonResult.structuredFilters(),
                pythonResult.needsClarification(),
                pythonResult.clarificationQuestion(),
                pythonResult.intentClassified(),
                pythonResult.processingTimeMs()
        );
    }
}
