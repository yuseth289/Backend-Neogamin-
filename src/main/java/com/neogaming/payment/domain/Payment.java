package com.neogaming.payment.domain;

import com.neogaming.common.audit.AuditableEntity;
import com.neogaming.common.enums.EstadoPago;
import com.neogaming.common.enums.TipoPago;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidad que representa un pago procesado por Mercado Pago Colombia.
 *
 * Ciclo de vida:
 *  PENDING    → MP procesando
 *  PROCESSING → MP en revisión (pagos PSE/Efecty pueden tardar)
 *  APPROVED   → Pago confirmado → se genera la orden
 *  REJECTED   → Pago rechazado → se libera el stock reservado
 *  CANCELLED  → Cancelado por el usuario o expirado
 *  REFUNDED   → Reembolso procesado
 *  IN_MEDIATION → Disputa abierta en MP
 *
 * El campo mpResponse guarda la respuesta raw de MP para debugging
 * y reconciliación contable. No se expone en la API.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends AuditableEntity {

    /** Identificador único del pago en NeoGaming */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** UUID del checkout al que corresponde este pago */
    @Column(name = "checkout_id", nullable = false, unique = true)
    private UUID checkoutId;

    /** UUID del usuario que realizó el pago */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * ID del pago asignado por Mercado Pago.
     * Se llena cuando MP confirma la creación del pago.
     * Usado para reconciliación y consultas a la API de MP.
     */
    @Column(name = "mp_payment_id", length = 100)
    private String mpPaymentId;

    /**
     * ID de la preferencia de pago creada en MP.
     * Se genera al iniciar el flujo y se usa para redirigir al usuario
     * al checkout de Mercado Pago.
     */
    @Column(name = "mp_preference_id", length = 100)
    private String mpPreferenceId;

    /**
     * Estado del pago según Mercado Pago.
     * Ejemplos: "pending", "approved", "rejected", "in_mediation"
     * Se sincroniza con el estado interno (EstadoPago) al recibir webhooks.
     */
    @Column(name = "mp_status", length = 30)
    private String mpStatus;

    /** Estado interno del pago en NeoGaming */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EstadoPago status = EstadoPago.PENDING;

    /** Método de pago usado por el comprador */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private TipoPago paymentMethod;

    /** Monto total pagado en COP */
    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    /**
     * Respuesta raw de Mercado Pago en formato JSONB.
     * Se guarda para debugging, reconciliación y auditoría.
     * No se expone en ningún endpoint de la API.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mp_response", columnDefinition = "jsonb")
    private Object mpResponse;
}
