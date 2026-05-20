package com.neogaming.catalog.offer.domain;

import com.neogaming.common.audit.AuditableEntity;
import com.neogaming.common.enums.EstadoGenerico;
import com.neogaming.common.enums.TipoDescuento;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidad que representa una oferta de descuento sobre un producto.
 *
 * Tipos de descuento:
 *  PERCENTAGE → descuento porcentual sobre el finalPrice del producto
 *               Ej: 20% OFF sobre $416.500 COP = $83.300 de descuento
 *  FIXED      → descuento de monto fijo en COP
 *               Ej: $50.000 COP de descuento sobre cualquier precio
 *
 * Solo puede haber UNA oferta ACTIVE por producto en un momento dado.
 * El campo discountedPrice se persiste para optimizar las consultas del
 * catálogo (evita recalcular en cada query de listado).
 *
 * Una oferta está vigente cuando:
 *   status = ACTIVE && now >= startDate && now <= endDate
 */
@Entity
@Table(name = "offers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offer extends AuditableEntity {

    /** Identificador único de la oferta */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Producto sobre el cual aplica esta oferta */
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /** Nombre descriptivo de la oferta (ej: "Black Friday 2026", "Promoción de verano") */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Tipo de descuento: PERCENTAGE (porcentaje) o FIXED (monto fijo en COP).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private TipoDescuento discountType;

    /**
     * Valor del descuento.
     * Si discountType = PERCENTAGE: valor entre 0 y 100 (ej: 20.00 = 20%)
     * Si discountType = FIXED: monto en COP (ej: 50000.00 = $50.000 COP)
     */
    @Column(name = "discount_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountValue;

    /**
     * Precio con descuento aplicado en COP.
     * Calculado al crear/actualizar la oferta y persistido para optimizar
     * las consultas del catálogo. Se recalcula si cambia el precio del producto.
     */
    @Column(name = "discounted_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountedPrice;

    /** Fecha y hora de inicio de vigencia de la oferta */
    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    /** Fecha y hora de fin de vigencia de la oferta */
    @Column(name = "end_date", nullable = false)
    private Instant endDate;

    /**
     * Estado de la oferta.
     * ACTIVE:   oferta vigente y aplicable.
     * INACTIVE: oferta desactivada manualmente.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EstadoGenerico status = EstadoGenerico.ACTIVE;

    /**
     * Verifica si la oferta está vigente en este momento.
     * La oferta debe estar ACTIVE y dentro del rango de fechas.
     *
     * @return true si la oferta está activa y dentro del período de vigencia
     */
    public boolean estaVigente() {
        Instant ahora = Instant.now();
        return status == EstadoGenerico.ACTIVE
                && !ahora.isBefore(startDate)
                && !ahora.isAfter(endDate);
    }
}
