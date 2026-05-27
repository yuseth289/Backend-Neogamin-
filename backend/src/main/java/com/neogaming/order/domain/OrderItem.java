package com.neogaming.order.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Ítem individual de una orden de compra.
 *
 * Los campos productName y productSku son snapshots del momento de la compra.
 * Si el vendedor actualiza el nombre del producto después, el historial
 * de la orden siempre refleja el nombre original al momento de comprar.
 */
@Entity
@Table(name = "order_items")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    /** Identificador único del ítem de la orden */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** UUID de la orden a la que pertenece */
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    /** UUID del grupo (por vendedor) al que pertenece este ítem */
    @Column(name = "order_group_id", nullable = false)
    private UUID orderGroupId;

    /** UUID del producto (referencia para consultas futuras) */
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /** Nombre del producto al momento de la compra (snapshot inmutable) */
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    /** SKU del producto al momento de la compra (puede ser null) */
    @Column(name = "product_sku", length = 100)
    private String productSku;

    /** Cantidad de unidades compradas */
    @Column(name = "quantity", nullable = false)
    private int quantity;

    /** Precio unitario pagado con IVA en COP */
    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    /** Subtotal del ítem: unitPrice × quantity en COP */
    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    /** Fecha de creación del ítem (inmutable) */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
