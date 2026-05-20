package com.neogaming.cart.domain;

import com.neogaming.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Ítem dentro del carrito de compras de un usuario.
 *
 * Almacena el precio unitario al momento de agregar el producto al carrito.
 * Este precio se compara con el precio actual al iniciar el checkout para
 * notificar al usuario si el precio cambió.
 *
 * Un producto solo puede aparecer una vez por carrito (constraint UNIQUE
 * en cart_id + product_id). Si el usuario agrega el mismo producto,
 * se incrementa la cantidad del ítem existente.
 */
@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem extends AuditableEntity {

    /** Identificador único del ítem del carrito */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** UUID del carrito al que pertenece este ítem */
    @Column(name = "cart_id", nullable = false)
    private UUID cartId;

    /** UUID del producto en este ítem */
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /** Cantidad de unidades del producto en el carrito */
    @Column(name = "quantity", nullable = false)
    private int quantity;

    /**
     * Precio unitario final (con IVA incluido) al momento de agregar al carrito.
     * Se guarda en COP con 2 decimales de precisión.
     * Permite detectar si el precio cambió antes de proceder al checkout.
     */
    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Calcula el subtotal de este ítem del carrito.
     * subtotal = unitPrice × quantity
     *
     * @return Precio total del ítem en COP
     */
    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
