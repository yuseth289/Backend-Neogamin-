package com.neogaming.checkout.repository;

import com.neogaming.checkout.domain.CheckoutItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad CheckoutItem.
 */
public interface CheckoutItemRepository extends JpaRepository<CheckoutItem, UUID> {

    /**
     * Lista todos los ítems de un checkout.
     *
     * @param checkoutId UUID del checkout
     * @return Lista de ítems del checkout
     */
    List<CheckoutItem> findByCheckoutId(UUID checkoutId);
}
