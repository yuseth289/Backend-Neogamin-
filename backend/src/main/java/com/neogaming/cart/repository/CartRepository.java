package com.neogaming.cart.repository;

import com.neogaming.cart.domain.Cart;
import com.neogaming.common.enums.EstadoCarrito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad Cart.
 */
public interface CartRepository extends JpaRepository<Cart, UUID> {

    /**
     * Busca el carrito activo del usuario.
     * Solo puede existir un carrito ACTIVE por usuario a la vez.
     *
     * @param userId UUID del usuario
     * @param status Estado del carrito (ACTIVE)
     * @return El carrito activo del usuario si existe
     */
    Optional<Cart> findByUserIdAndStatus(UUID userId, EstadoCarrito status);
}
