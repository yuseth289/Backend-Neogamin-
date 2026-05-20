package com.neogaming.order.mapper;

import com.neogaming.order.domain.Order;
import com.neogaming.order.domain.OrderGroup;
import com.neogaming.order.domain.OrderItem;
import com.neogaming.order.dto.response.OrderGroupResponse;
import com.neogaming.order.dto.response.OrderItemResponse;
import com.neogaming.order.dto.response.OrderResponse;
import com.neogaming.order.dto.response.OrderSummaryResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mapper para convertir entidades de órdenes a sus DTOs de respuesta.
 */
@Component
public class OrderMapper {

    /**
     * Convierte una orden completa con sus grupos e ítems al DTO de respuesta.
     *
     * @param order      Entidad de la orden
     * @param groups     Lista de grupos de la orden
     * @param itemsByGroup Mapa de groupId → lista de ítems del grupo
     * @return DTO completo de la orden
     */
    public OrderResponse toResponse(Order order, List<OrderGroup> groups,
                                    Map<UUID, List<OrderItem>> itemsByGroup) {
        List<OrderGroupResponse> groupResponses = groups.stream()
                .map(group -> {
                    List<OrderItem> items = itemsByGroup.getOrDefault(group.getId(), List.of());
                    List<OrderItemResponse> itemResponses = items.stream()
                            .map(this::toItemResponse)
                            .toList();
                    return new OrderGroupResponse(
                            group.getId(),
                            group.getSellerId(),
                            group.getStatus(),
                            group.getSubtotal(),
                            group.getTrackingNumber(),
                            itemResponses
                    );
                })
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                groupResponses,
                order.getShippingAddress(),
                order.getSubtotal(),
                order.getShippingCost(),
                order.getTotal(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    /**
     * Convierte una orden a su versión resumida para listados.
     *
     * @param order      Entidad de la orden
     * @param totalItems Total de unidades en la orden
     * @return DTO resumido de la orden
     */
    public OrderSummaryResponse toSummaryResponse(Order order, int totalItems) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getStatus(),
                totalItems,
                order.getTotal(),
                order.getCreatedAt()
        );
    }

    /**
     * Convierte un OrderItem a su DTO de respuesta.
     *
     * @param item Entidad del ítem de la orden
     * @return DTO del ítem
     */
    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId(),
                item.getProductName(),
                item.getProductSku(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }
}
