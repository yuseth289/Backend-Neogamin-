package com.neogaming.cart.repository;

import com.neogaming.cart.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad CartItem.
 */
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    /**
     * Lista todos los ítems de un carrito.
     *
     * @param cartId UUID del carrito
     * @return Lista de ítems del carrito
     */
    List<CartItem> findByCartId(UUID cartId);

    /**
     * Busca un ítem específico en un carrito por producto.
     * Usado para verificar si el producto ya está en el carrito
     * y actualizar la cantidad en lugar de duplicar el ítem.
     *
     * @param cartId    UUID del carrito
     * @param productId UUID del producto
     * @return El ítem si existe
     */
    Optional<CartItem> findByCartIdAndProductId(UUID cartId, UUID productId);

    /**
     * Elimina todos los ítems de un carrito.
     * Usado cuando el usuario vacía el carrito o al convertirlo en checkout.
     *
     * @param cartId UUID del carrito
     */
    void deleteByCartId(UUID cartId);
}
