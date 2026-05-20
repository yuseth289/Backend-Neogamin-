package com.neogaming.inventory.domain;

import com.neogaming.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entidad que representa el inventario de un producto en NeoGaming.
 *
 * Modelo de stock:
 *  - physicalStock:  unidades físicamente disponibles en bodega
 *  - reservedStock:  unidades bloqueadas por checkouts pendientes de pago
 *  - availableStock: physicalStock - reservedStock (calculado, no persistido)
 *
 * Invariante: reservedStock <= physicalStock (garantizado por constraint en BD)
 *
 * Flujo de una compra:
 *  1. Cliente inicia checkout → reservedStock += cantidad
 *  2a. Pago exitoso → physicalStock -= cantidad, reservedStock -= cantidad
 *  2b. Checkout expira/pago falla → reservedStock -= cantidad (liberación)
 */
@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory extends AuditableEntity {

    /** Identificador único del registro de inventario */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Producto al que pertenece este inventario.
     * Relación 1:1 — un producto tiene exactamente un registro de inventario.
     */
    @Column(name = "product_id", nullable = false, unique = true)
    private UUID productId;

    /**
     * Stock físico: unidades reales disponibles en bodega del vendedor.
     * Se reduce cuando una orden es confirmada y pagada.
     */
    @Column(name = "physical_stock", nullable = false)
    @Builder.Default
    private int physicalStock = 0;

    /**
     * Stock reservado: unidades bloqueadas por checkouts activos pendientes de pago.
     * Se incrementa al crear un checkout y se libera o confirma según el resultado del pago.
     */
    @Column(name = "reserved_stock", nullable = false)
    @Builder.Default
    private int reservedStock = 0;

    /**
     * Calcula el stock disponible para nuevas compras.
     * Se calcula en tiempo real — no se persiste en la BD.
     *
     * @return physicalStock - reservedStock
     */
    public int getAvailableStock() {
        return physicalStock - reservedStock;
    }

    /**
     * Verifica si hay suficiente stock disponible para una cantidad dada.
     *
     * @param cantidad Unidades a verificar
     * @return true si hay suficiente stock disponible
     */
    public boolean tieneStockDisponible(int cantidad) {
        return getAvailableStock() >= cantidad;
    }
}
