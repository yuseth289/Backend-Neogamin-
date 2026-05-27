package com.neogaming.order.domain;

import com.neogaming.common.audit.AuditableEntity;
import com.neogaming.common.enums.EstadoGrupo;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Grupo de ítems de una orden, agrupados por vendedor.
 *
 * Una orden puede contener productos de múltiples vendedores.
 * Cada grupo representa el sub-pedido de un vendedor específico
 * y tiene su propio estado de preparación y envío.
 *
 * El split de pago de Mercado Pago opera a nivel de grupo:
 * cada vendedor recibe el pago de su grupo (menos la comisión de NeoGaming).
 *
 * Ciclo de vida del grupo:
 *  PENDING  → CONFIRMED → PREPARING → SHIPPED → DELIVERED
 *  (cualquier estado) → CANCELLED
 */
@Entity
@Table(name = "order_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderGroup extends AuditableEntity {

    /** Identificador único del grupo */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** UUID de la orden a la que pertenece este grupo */
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    /** UUID del vendedor responsable de este grupo */
    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    /** Estado actual del grupo para este vendedor */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private EstadoGrupo status = EstadoGrupo.PENDING;

    /** Subtotal de los ítems de este grupo en COP */
    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    /**
     * Número de seguimiento del envío.
     * Lo agrega el vendedor cuando marca el grupo como SHIPPED.
     */
    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;
}
