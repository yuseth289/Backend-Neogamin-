package com.neogaming.wishlist.repository;

import com.neogaming.wishlist.domain.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad WishlistItem.
 */
public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {

    /** Lista todos los ítems de una lista. */
    List<WishlistItem> findByWishlistId(UUID wishlistId);

    /** Busca un ítem específico dentro de una lista. */
    Optional<WishlistItem> findByWishlistIdAndProductId(UUID wishlistId, UUID productId);

    /** Verifica si el producto ya está en la lista. */
    boolean existsByWishlistIdAndProductId(UUID wishlistId, UUID productId);

    /** Elimina todos los ítems de una lista — usado al borrar la lista completa. */
    void deleteByWishlistId(UUID wishlistId);
}
