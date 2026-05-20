package com.neogaming.checkout.mapper;

import com.neogaming.checkout.domain.Checkout;
import com.neogaming.checkout.domain.CheckoutItem;
import com.neogaming.checkout.dto.response.CheckoutItemResponse;
import com.neogaming.checkout.dto.response.CheckoutResponse;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mapper para convertir entidades Checkout y CheckoutItem a sus DTOs.
 */
@Component
public class CheckoutMapper {

    /**
     * Convierte un Checkout a su DTO de respuesta.
     * Calcula los minutos restantes antes de expiración.
     *
     * @param checkout Entidad del checkout
     * @param items    Lista de ítems del checkout
     * @param productNames Mapa de productId → nombre del producto (para desnormalizar)
     * @return DTO del checkout con minutesLeft calculado
     */
    public CheckoutResponse toResponse(Checkout checkout, List<CheckoutItem> items,
                                       Map<UUID, String> productNames) {
        List<CheckoutItemResponse> itemResponses = items.stream()
                .map(item -> new CheckoutItemResponse(
                        item.getProductId(),
                        productNames.getOrDefault(item.getProductId(), "Producto"),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .toList();

        // Calcular minutos restantes (mínimo 0 para no mostrar negativos)
        long minutesLeft = Math.max(0,
                Duration.between(Instant.now(), checkout.getExpiresAt()).toMinutes()
        );

        return new CheckoutResponse(
                checkout.getId(),
                checkout.getStatus(),
                itemResponses,
                checkout.getSubtotal(),
                checkout.getShippingCost(),
                checkout.getTotal(),
                checkout.getPaymentMethod(),
                checkout.getExpiresAt(),
                minutesLeft,
                checkout.getCreatedAt()
        );
    }
}
