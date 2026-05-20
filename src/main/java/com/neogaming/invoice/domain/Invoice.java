package com.neogaming.invoice.domain;

import com.neogaming.common.audit.AuditableEntity;
import com.neogaming.common.enums.EstadoFactura;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidad que representa una factura electrónica emitida al comprador.
 *
 * Se genera automáticamente cuando Mercado Pago aprueba el pago.
 * Cada orden tiene exactamente una factura que cubre todos sus ítems.
 *
 * El campo items almacena un snapshot JSONB de los productos facturados,
 * garantizando que el registro contable sea inmutable aunque los precios
 * o nombres de los productos cambien después.
 *
 * Ciclo de vida:
 *  DRAFT     → generada internamente, pendiente de envío al comprador
 *  ISSUED    → enviada al correo del comprador
 *  CANCELLED → anulada por devolución, error o solicitud del comprador
 */
@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice extends AuditableEntity {

    /** Identificador único de la factura en NeoGaming */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** UUID de la orden que originó esta factura */
    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    /** UUID del pago confirmado que activó la generación de la factura */
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    /** UUID del comprador */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Número de factura legible por humanos.
     * Formato: NG-{AÑO}-{SECUENCIAL 6 dígitos} — ej. NG-2026-000001
     * Único a nivel global.
     */
    @Column(name = "invoice_number", nullable = false, length = 30, unique = true)
    private String invoiceNumber;

    /** Estado actual de la factura */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EstadoFactura status = EstadoFactura.DRAFT;

    /** Nombre completo del comprador al momento de la compra */
    @Column(name = "buyer_name", nullable = false, length = 200)
    private String buyerName;

    /** Correo electrónico del comprador al momento de la compra */
    @Column(name = "buyer_email", nullable = false, length = 200)
    private String buyerEmail;

    /** Número de documento del comprador (CC, NIT, CE, TI) — puede ser nulo */
    @Column(name = "buyer_document", length = 20)
    private String buyerDocument;

    /** Suma de (unitPrice × quantity) de todos los ítems antes de impuestos */
    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    /** Monto de IVA calculado sobre el subtotal (tarifa estándar 19% en Colombia) */
    @Column(name = "tax_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    /** Total facturado en COP: subtotal + taxAmount */
    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    /**
     * Snapshot JSONB de los ítems facturados.
     * Estructura de cada ítem:
     * { productId, productName, productSku, quantity, unitPrice, subtotal }
     * Inmutable históricamente — refleja los precios del momento de compra.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private Object items = java.util.List.of();

    /** Fecha y hora en que la factura fue enviada al comprador */
    @Column(name = "issued_at")
    private Instant issuedAt;

    /** Fecha y hora de anulación de la factura */
    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    /** Motivo de anulación — obligatorio al cancelar */
    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;
}
