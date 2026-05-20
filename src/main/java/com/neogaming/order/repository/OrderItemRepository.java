package com.neogaming.order.repository;

import com.neogaming.order.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad OrderItem.
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    /**
     * Lista todos los ítems de una orden.
     *
     * @param orderId UUID de la orden
     * @return Lista de ítems de la orden
     */
    List<OrderItem> findByOrderId(UUID orderId);

    /**
     * Lista los ítems de un grupo específico de la orden.
     *
     * @param orderGroupId UUID del grupo
     * @return Lista de ítems del grupo
     */
    List<OrderItem> findByOrderGroupId(UUID orderGroupId);
}
