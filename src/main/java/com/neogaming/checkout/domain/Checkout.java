package com.neogaming.checkout.domain;

import com.neogaming.common.audit.AuditableEntity;
import com.neogaming.common.enums.EstadoCheckout;
import com.neogaming.common.enums.TipoPago;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Entidad que representa una sesión de pago (checkout) en NeoGaming.
 *
 * El checkout es el puente entre el carrito y la orden definitiva.
 * Tiene un tiempo de vida limitado (30 min por defecto) durante el cual
 * el stock está reservado para el usuario.
 *
 * Los snapshots JSONB capturan el estado inmutable al momento del checkout:
 *  - itemsSnapshot: productos, precios y cantidades acordadas
 *  - shippingAddress: dirección de entrega tal como fue seleccionada
 *
 * Esto garantiza que si el precio cambia después de que el usuario inició
 * el pago, la orden refleje el precio al que acordó pagar.
 *
 * Ciclo de vida:
 *  PENDING → [pago exitoso] → COMPLETED
 *  PENDING → [expiró]       → EXPIRED  (batch job libera stock)
 *  PENDING → [cancelado]    → CANCELLED (usuario cancela)
 */
@Entity
@Table(name = "checkouts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Checkout extends AuditableEntity {

    /** Identificador único del checkout */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** UUID del usuario que inició el checkout */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** UUID del carrito que originó este checkout (para historial) */
    @Column(name = "cart_id")
    private UUID cartId;

    /**
     * Estado del checkout.
     * PENDING:   activo y pendiente de pago.
     * COMPLETED: pago confirmado → se creó la orden.
     * EXPIRED:   expiró sin pago.
     * CANCELLED: cancelado explícitamente por el usuario.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EstadoCheckout status = EstadoCheckout.IN_PROGRESS;

    /**
     * Snapshot JSONB inmutable de los ítems al momento de crear el checkout.
     * Almacenado como JSON en PostgreSQL para consultas flexibles.
     * Formato: lista de objetos con productId, name, quantity, unitPrice.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items_snapshot", nullable = false, columnDefinition = "jsonb")
    private Object itemsSnapshot;

    /**
     * Snapshot JSONB de la dirección de entrega seleccionada.
     * Capturado en el momento del checkout para que no cambie si el usuario
     * modifica su dirección después.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shipping_address", columnDefinition = "jsonb")
    private Object shippingAddress;

    /** Subtotal antes de gastos de envío en COP */
    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    /** Costo de envío en COP (0 si envío gratuito) */
    @Column(name = "shipping_cost", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal shippingCost = BigDecimal.ZERO;

    /** Total a pagar en COP (subtotal + shippingCost) */
    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    /** Método de pago seleccionado por el usuario */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private TipoPago paymentMethod;

    /**
     * Momento en que el checkout expira automáticamente.
     * Configurado en app.business.checkout-expiry-minutes (default 30 min).
     * Al expirar, el batch job libera el stock reservado.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Verifica si el checkout ha expirado comparando con el momento actual.
     *
     * @return true si el checkout ya expiró
     */
    public boolean haExpirado() {
        return Instant.now().isAfter(expiresAt);
    }
}
