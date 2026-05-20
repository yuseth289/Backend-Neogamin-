package com.neogaming.catalog.product.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad que representa una imagen de un producto en NeoGaming.
 *
 * Un producto puede tener múltiples imágenes. Solo una puede ser la
 * imagen principal (isPrimary = true). Las demás conforman la galería
 * y se ordenan por sortOrder ascendente.
 *
 * Las URLs apuntan a imágenes almacenadas en CDN/S3.
 * La subida de imágenes se maneja externamente (ej: presigned URLs de S3).
 */
@Entity
@Table(name = "product_images")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    /** Identificador único de la imagen */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** UUID del producto al que pertenece esta imagen */
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /** URL completa de la imagen en CDN/S3 */
    @Column(name = "url", nullable = false, length = 500)
    private String url;

    /** Texto alternativo para accesibilidad y SEO */
    @Column(name = "alt_text", length = 200)
    private String altText;

    /**
     * Posición de la imagen en la galería del producto.
     * Las imágenes se muestran ordenadas por sortOrder ascendente.
     * La imagen principal (isPrimary = true) siempre aparece primera.
     */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    /**
     * Indica si esta es la imagen principal del producto.
     * Solo una imagen por producto puede ser la principal.
     * Se muestra en listados del catálogo y como primera en el detalle.
     */
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean primary = false;

    /** Fecha de subida de la imagen */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
