package com.neogaming.internal.product.controller;

import com.neogaming.catalog.product.domain.Product;
import com.neogaming.catalog.product.repository.ProductImageRepository;
import com.neogaming.catalog.product.repository.ProductRepository;
import com.neogaming.catalog.category.repository.CategoryRepository;
import com.neogaming.catalog.category.mapper.CategoryMapper;
import com.neogaming.catalog.category.dto.response.CategoryResponse;
import com.neogaming.catalog.product.mapper.ProductMapper;
import com.neogaming.catalog.product.dto.response.ProductSummaryResponse;
import com.neogaming.common.enums.EstadoProducto;
import com.neogaming.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints internos para el microservicio Python AI.
 * Solo accesibles con X-Internal-Token (validado por InternalTokenFilter).
 * Nunca llamados desde el frontend Angular.
 */
@RestController
@RequestMapping("/internal/products")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;

    @GetMapping("/search")
    public ResponseEntity<PageResponse<ProductSummaryResponse>> searchProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<Product> products;

        if (q != null && !q.isBlank()) {
            products = productRepository.buscarPorTextoYEstado(q, EstadoProducto.ACTIVE, pageable);
        } else {
            products = productRepository.findByStatus(EstadoProducto.ACTIVE, pageable);
        }

        Page<ProductSummaryResponse> mapped = products.map(p -> {
            String primaryImageUrl = productImageRepository
                    .findByProductIdAndPrimaryTrue(p.getId())
                    .map(img -> img.getUrl())
                    .orElse(null);
            return productMapper.toSummaryResponse(p, primaryImageUrl);
        });

        return ResponseEntity.ok(PageResponse.from(mapped));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductSummaryResponse> getProductById(@PathVariable UUID id) {
        return productRepository.findById(id)
                .filter(p -> p.getStatus() == EstadoProducto.ACTIVE)
                .map(p -> {
                    String primaryImageUrl = productImageRepository
                            .findByProductIdAndPrimaryTrue(p.getId())
                            .map(img -> img.getUrl())
                            .orElse(null);
                    return productMapper.toSummaryResponse(p, primaryImageUrl);
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        return ResponseEntity.ok(
                categoryRepository.findAll().stream()
                        .map(categoryMapper::toResponse)
                        .toList()
        );
    }
}
