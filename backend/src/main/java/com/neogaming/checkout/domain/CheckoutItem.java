package com.neogaming.checkout.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Ítem relacional del checkout para consultas SQL de conciliación.
 *
 * Los datos también están en el JSONB itemsSnapshot del Checkout,
 * pero esta tabla relacional facilita queries de reporte por vendedor,
 * cálculo de comisiones y conciliación contable.
 *
 * El sellerId está desnormalizado aquí para facilitar la lógica del
 * split de pago de Mercado Pago (cada ítem va a un vendedor distinto).
 */
@Entity
@Table(name = "checkout_items")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutItem {

    /** Identificador único del ítem del checkout */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** UUID del checkout al que pertenece este ítem */
    @Column(name = "checkout_id", nullable = false)
    private UUID checkoutId;

    /** UUID del producto comprado */
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /**
     * UUID del vendedor del producto.
     * Desnormalizado para facilitar la división del pago (split)
     * entre múltiples vendedores en un mismo checkout.
     */
    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    /** Cantidad de unidades compradas */
    @Column(name = "quantity", nullable = false)
    private int quantity;

    /** Precio unitario con IVA al momento del checkout en COP */
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
