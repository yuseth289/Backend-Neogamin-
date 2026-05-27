package com.neogaming.review.domain;

import com.neogaming.common.audit.AuditableEntity;
import com.neogaming.common.enums.EstadoResena;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entidad que representa la reseña de un comprador sobre un producto.
 *
 * Restricciones de negocio:
 *  - Solo compradores que hayan comprado el producto pueden reseñarlo.
 *  - Máximo una reseña por usuario por producto (restricción UNIQUE en BD).
 *  - La calificación debe estar entre 1 y 5 estrellas.
 *
 * Ciclo de vida:
 *  PENDING  → recién creada, esperando moderación
 *  APPROVED → visible en la página del producto
 *  REJECTED → rechazada (inapropiada, spam, lenguaje ofensivo)
 */
@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review extends AuditableEntity {

    /** Identificador único de la reseña */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** UUID del producto reseñado */
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /** UUID del comprador que escribió la reseña */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** UUID de la orden en la que se compró el producto (nullable en datos de demo/admin) */
    @Column(name = "order_id")
    private UUID orderId;

    /** Calificación de 1 a 5 estrellas */
    @Column(name = "rating", nullable = false)
    private int rating;

    /** Título breve de la reseña (opcional) */
    @Column(name = "title", length = 150)
    private String title;

    /** Contenido detallado de la reseña (opcional) */
    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    /** Estado de moderación de la reseña */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EstadoResena status = EstadoResena.PENDING;

    /** Motivo por el que el moderador rechazó la reseña (solo cuando status = REJECTED) */
    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    /** Nombre de visualización para reseñas creadas por el admin (reemplaza el nombre del usuario) */
    @Column(name = "buyer_name_override", length = 150)
    private String buyerNameOverride;
}
