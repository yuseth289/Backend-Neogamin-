package com.neogaming.order.domain;

import com.neogaming.common.audit.AuditableEntity;
import com.neogaming.common.enums.EstadoPedido;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidad que representa una orden de compra en NeoGaming.
 *
 * La orden es el registro permanente de una compra completada.
 * Se crea a partir de un checkout cuando el pago es confirmado por Mercado Pago.
 *
 * Los snapshots JSONB capturan el estado al momento de la compra,
 * garantizando que el registro histórico sea inmutable incluso si
 * los datos del producto o la dirección cambian después.
 *
 * Ciclo de vida:
 *  PENDING          → estado inicial
 *  PAYMENT_PENDING  → esperando confirmación de Mercado Pago
 *  PAYMENT_APPROVED → pago aprobado → vendedores preparando
 *  PROCESSING       → al menos un grupo en preparación
 *  PARTIALLY_SHIPPED → algunos grupos enviados
 *  SHIPPED          → todos los grupos enviados
 *  DELIVERED        → todos los grupos entregados
 *  CANCELLED        → cancelada antes del envío
 *  REFUNDED         → reembolso procesado
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends AuditableEntity {

    /** Identificador único de la orden */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** UUID del comprador */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** UUID del checkout que generó esta orden */
    @Column(name = "checkout_id")
    private UUID checkoutId;

    /**
     * Estado global de la orden.
     * Se actualiza conforme los grupos (por vendedor) avanzan en su estado.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private EstadoPedido status = EstadoPedido.PENDING;

    /**
     * Snapshot JSONB de la dirección de entrega al momento de la compra.
     * Copiado del checkout — inmutable históricamente.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shipping_address", nullable = false, columnDefinition = "jsonb")
    private Object shippingAddress;

    /** Subtotal sin gastos de envío en COP */
    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    /** Costo de envío en COP */
    @Column(name = "shipping_cost", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal shippingCost = BigDecimal.ZERO;

    /** Total pagado en COP (subtotal + shippingCost) */
    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    /**
     * UUID del registro de pago en la tabla payments.
     * Se llena cuando Mercado Pago confirma el pago.
     */
    @Column(name = "payment_id")
    private UUID paymentId;
}
