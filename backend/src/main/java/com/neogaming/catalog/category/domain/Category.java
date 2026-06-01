package com.neogaming.catalog.category.domain;

import com.neogaming.common.audit.AuditableEntity;
import com.neogaming.common.enums.EstadoGenerico;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entidad que representa una categoría de productos en NeoGaming.
 *
 * Soporta una jerarquía de dos niveles:
 *  - Categoría padre: parentId = null (ej: "Periféricos", "Consolas")
 *  - Subcategoría:    parentId = UUID del padre (ej: "Teclados", "PlayStation")
 *
 * Las categorías inactivas (status = INACTIVE) no aparecen en el catálogo
 * público pero siguen existiendo para no romper productos ya categorizados.
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category extends AuditableEntity {

    /** Identificador único de la categoría */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Nombre visible de la categoría (ej: "Teclados Mecánicos") */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Slug URL-amigable único (ej: "teclados-mecanicos").
     * Generado automáticamente a partir del nombre.
     * Usado en filtros de búsqueda: GET /products?category=teclados-mecanicos
     */
    @Column(name = "slug", nullable = false, unique = true, length = 120)
    private String slug;

    /** Descripción opcional de la categoría */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** URL de imagen representativa (almacenada en CDN/S3) */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** Nombre del ícono Lucide asociado (ej: lucideGamepad2). Nulo = ícono por defecto. */
    @Column(name = "icon_name", length = 100)
    private String iconName;

    /**
     * UUID de la categoría padre para la jerarquía de dos niveles.
     * null = categoría raíz (padre).
     * UUID = subcategoría de ese padre.
     */
    @Column(name = "parent_id")
    private UUID parentId;

    /**
     * Estado de la categoría.
     * ACTIVE:   visible en el catálogo público.
     * INACTIVE: oculta, pero los productos ya categorizados siguen existiendo.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EstadoGenerico status = EstadoGenerico.ACTIVE;
}
