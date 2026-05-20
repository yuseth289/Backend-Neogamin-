package com.neogaming.order.repository;

import com.neogaming.order.domain.OrderGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad OrderGroup.
 */
public interface OrderGroupRepository extends JpaRepository<OrderGroup, UUID> {

    /**
     * Lista todos los grupos de una orden.
     *
     * @param orderId UUID de la orden
     * @return Lista de grupos de la orden
     */
    List<OrderGroup> findByOrderId(UUID orderId);

    /**
     * Lista los grupos de una orden que pertenecen a un vendedor específico.
     * Usado por el vendedor para ver sus pedidos pendientes.
     *
     * @param sellerId UUID del vendedor
     * @return Lista de grupos del vendedor
     */
    List<OrderGroup> findBySellerId(UUID sellerId);

    /**
     * Busca un grupo verificando que pertenezca a la orden y al vendedor.
     *
     * @param id       UUID del grupo
     * @param orderId  UUID de la orden
     * @param sellerId UUID del vendedor
     * @return El grupo si existe y cumple los filtros
     */
    Optional<OrderGroup> findByIdAndOrderIdAndSellerId(UUID id, UUID orderId, UUID sellerId);
}
