package com.neogaming.inventory.domain;

import com.neogaming.common.enums.TipoMovimientoStock;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Registra cada cambio en el inventario de un producto.
 *
 * Es un log inmutable — los movimientos NUNCA se editan ni eliminan.
 * Son la fuente de verdad para auditoría, disputas y conciliación contable.
 *
 * Tipos de movimiento (TipoMovimientoStock):
 *  IN      → Entrada de mercancía al inventario (vendedor agrega stock)
 *  OUT     → Salida confirmada (pago exitoso, stock reducido definitivamente)
 *  RESERVE → Stock reservado al crear un checkout
 *  RELEASE → Reserva liberada (checkout expirado o pago fallido)
 *  ADJUST  → Ajuste manual del vendedor (ej: corrección por inventario físico)
 */
@Entity
@Table(name = "inventory_movements")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovement {

    /** Identificador único del movimiento */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Inventario al que pertenece este movimiento */
    @Column(name = "inventory_id", nullable = false)
    private UUID inventoryId;

    /** Producto involucrado (desnormalizado para facilitar consultas de auditoría) */
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /** Tipo de movimiento que ocurrió */
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private TipoMovimientoStock movementType;

    /**
     * Cantidad de unidades del movimiento (siempre positiva).
     * El tipo de movimiento indica la dirección (entrada o salida).
     */
    @Column(name = "quantity", nullable = false)
    private int quantity;

    /** Stock físico resultante después del movimiento (para auditoría rápida) */
    @Column(name = "physical_after", nullable = false)
    private int physicalAfter;

    /** Stock reservado resultante después del movimiento */
    @Column(name = "reserved_after", nullable = false)
    private int reservedAfter;

    /**
     * UUID del documento que originó el movimiento.
     * Puede ser: UUID del checkout (RESERVE/RELEASE), UUID de la orden (OUT), null (ADJUST/IN).
     */
    @Column(name = "reference_id")
    private UUID referenceId;

    /** Descripción del motivo del movimiento (especialmente para ajustes manuales) */
    @Column(name = "notes", length = 300)
    private String notes;

    /** Fecha y hora exacta del movimiento (inmutable — no se actualiza) */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
