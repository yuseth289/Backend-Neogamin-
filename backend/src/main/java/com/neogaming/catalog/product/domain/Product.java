package com.neogaming.catalog.product.domain;

import com.neogaming.common.audit.AuditableEntity;
import com.neogaming.common.enums.EstadoProducto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Entidad que representa un producto en el catálogo de NeoGaming.
 *
 * Modelo de precios con IVA colombiano:
 *  - basePrice:  precio de venta sin IVA
 *  - ivaPercent: porcentaje de IVA (19% por defecto según DIAN Colombia)
 *  - finalPrice: calculado en el servicio → basePrice × (1 + ivaPercent/100)
 *
 * El precio final NO se persiste en la BD para siempre reflejar el precio
 * actual. Se captura como snapshot en el momento de crear el checkout.
 *
 * Ciclo de vida:
 *  DRAFT → [publicar] → ACTIVE
 *  ACTIVE → [pausar] → PAUSED
 *  PAUSED → [reactivar] → ACTIVE
 *  ACTIVE|PAUSED → [eliminar] → DELETED
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends AuditableEntity {

    /** Identificador único del producto */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** UUID del vendedor propietario del producto */
    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    /**
     * UUID de la categoría del producto.
     * Puede ser null si la categoría fue desactivada.
     */
    @Column(name = "category_id")
    private UUID categoryId;

    // ===== Datos del producto =====

    /** Nombre del producto visible en el catálogo */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * Slug URL-amigable único (generado desde name + sellerId).
     * Usado en URLs de producto: /products/{slug}
     */
    @Column(name = "slug", nullable = false, unique = true, length = 250)
    private String slug;

    /** Descripción detallada del producto (HTML permitido en producción) */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Marca del producto (ej: "Logitech", "Samsung", "Razer") */
    @Column(name = "brand", length = 100)
    private String brand;

    /**
     * Código SKU interno del vendedor para identificar el producto.
     * No es único a nivel global — puede repetirse entre vendedores.
     */
    @Column(name = "sku", length = 100)
    private String sku;

    // ===== Precios en COP =====

    /**
     * Precio base del producto en COP (sin IVA).
     * Se usa NUMERIC(14,2) → BigDecimal para precisión monetaria exacta.
     */
    @Column(name = "base_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal basePrice;

    /**
     * Porcentaje de IVA aplicable al producto.
     * Por defecto 19% según la DIAN Colombia.
     * Algunos productos tienen IVA del 0% o 5% (bienes esenciales).
     */
    @Column(name = "iva_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal ivaPercent = new BigDecimal("19.00");

    // ===== Estado =====

    /**
     * Estado actual del producto en el ciclo de vida.
     * Nuevo producto inicia en DRAFT hasta que el vendedor lo publique.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EstadoProducto status = EstadoProducto.DRAFT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "specifications", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> specifications = new HashMap<>();
}
