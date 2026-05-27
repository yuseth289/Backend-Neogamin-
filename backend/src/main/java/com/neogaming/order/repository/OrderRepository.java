package com.neogaming.order.repository;

import com.neogaming.common.enums.EstadoPedido;
import com.neogaming.order.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad Order.
 */
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Lista las órdenes de un usuario con paginación.
     * Ordenadas de más reciente a más antigua por defecto.
     *
     * @param userId   UUID del comprador
     * @param pageable Paginación
     * @return Página de órdenes del usuario
     */
    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Busca una orden verificando que pertenezca al usuario.
     * Previene que un usuario vea órdenes de otro.
     *
     * @param id     UUID de la orden
     * @param userId UUID del comprador
     * @return La orden si existe y pertenece al usuario
     */
    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Lista todas las órdenes con un estado dado (panel de administración).
     *
     * @param status   Estado a filtrar
     * @param pageable Paginación
     * @return Página de órdenes con ese estado
     */
    Page<Order> findByStatus(EstadoPedido status, Pageable pageable);

    /**
     * Busca una orden por el UUID del checkout que la originó.
     * Usado para vincular el pago confirmado con la orden correspondiente.
     *
     * @param checkoutId UUID del checkout
     * @return La orden si existe
     */
    Optional<Order> findByCheckoutId(UUID checkoutId);
}
