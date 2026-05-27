package com.neogaming.catalog.product.mapper;

import com.neogaming.catalog.product.domain.Product;
import com.neogaming.catalog.product.domain.ProductImage;
import com.neogaming.catalog.product.dto.response.ProductImageResponse;
import com.neogaming.catalog.product.dto.response.ProductResponse;
import com.neogaming.catalog.product.dto.response.ProductSummaryResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Mapper para convertir entidades Product y ProductImage a sus DTOs de respuesta.
 *
 * Responsable del cálculo del precio final con IVA:
 *   finalPrice = basePrice × (1 + ivaPercent / 100)
 *   Redondeado a 2 decimales con HALF_UP (estándar contable).
 */
@Component
public class ProductMapper {

    /**
     * Convierte un producto con su lista de imágenes al DTO completo.
     * Incluye descripción, todas las imágenes y precios calculados.
     *
     * @param product Entidad del producto
     * @param images  Lista de imágenes ya cargadas del producto
     * @return DTO completo del producto
     */
    public ProductResponse toResponse(Product product, List<ProductImage> images) {
        List<ProductImageResponse> imageResponses = images.stream()
                .map(this::toImageResponse)
                .toList();

        return new ProductResponse(
                product.getId(),
                product.getSellerId(),
                product.getCategoryId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getBrand(),
                product.getSku(),
                product.getBasePrice(),
                product.getIvaPercent(),
                calcularPrecioFinal(product.getBasePrice(), product.getIvaPercent()),
                product.getStatus(),
                imageResponses,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    /**
     * Convierte un producto a su versión resumida para listados del catálogo.
     * Solo incluye la imagen principal para optimizar la respuesta.
     *
     * @param product          Entidad del producto
     * @param primaryImageUrl  URL de la imagen principal (null si no tiene)
     * @return DTO resumido para tarjeta de producto
     */
    public ProductSummaryResponse toSummaryResponse(Product product, String primaryImageUrl) {
        return new ProductSummaryResponse(
                product.getId(),
                product.getSellerId(),
                product.getCategoryId(),
                product.getName(),
                product.getSlug(),
                product.getBrand(),
                product.getBasePrice(),
                calcularPrecioFinal(product.getBasePrice(), product.getIvaPercent()),
                product.getStatus(),
                primaryImageUrl
        );
    }

    /**
     * Convierte una entidad ProductImage a su DTO de respuesta.
     *
     * @param image Entidad de la imagen
     * @return DTO de la imagen
     */
    public ProductImageResponse toImageResponse(ProductImage image) {
        return new ProductImageResponse(
                image.getId(),
                image.getUrl(),
                image.getAltText(),
                image.getSortOrder(),
                image.isPrimary()
        );
    }

    /**
     * Calcula el precio final con IVA incluido.
     *
     * Fórmula: finalPrice = basePrice × (1 + ivaPercent / 100)
     * Ejemplo: 350.000 COP × (1 + 19/100) = 416.500 COP
     *
     * Se usa RoundingMode.HALF_UP que es el estándar contable colombiano.
     *
     * @param basePrice  Precio base sin IVA
     * @param ivaPercent Porcentaje de IVA (ej: 19.00)
     * @return Precio final con IVA, redondeado a 2 decimales
     */
    private BigDecimal calcularPrecioFinal(BigDecimal basePrice, BigDecimal ivaPercent) {
        BigDecimal multiplicador = BigDecimal.ONE.add(
                ivaPercent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
        );
        return basePrice.multiply(multiplicador).setScale(2, RoundingMode.HALF_UP);
    }
}
