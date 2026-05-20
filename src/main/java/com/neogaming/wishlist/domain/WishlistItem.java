package com.neogaming.wishlist.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Producto guardado dentro de una lista de deseos.
 * La restricción UNIQUE (wishlist_id, product_id) garantiza que el mismo
 * producto no se repita dentro de la misma lista.
 */
@Entity
@Table(name = "wishlist_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistItem {

    /** Identificador único del ítem */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** UUID de la lista a la que pertenece este ítem */
    @Column(name = "wishlist_id", nullable = false)
    private UUID wishlistId;

    /** UUID del producto guardado */
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /** Fecha y hora en que se agregó el producto a la lista */
    @Column(name = "added_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant addedAt = Instant.now();
}
