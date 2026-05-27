package com.neogaming.wishlist.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Lista de deseos de un usuario.
 * Puede ser privada (solo el dueño) o pública (compartible por enlace).
 * Un usuario puede tener múltiples listas con diferentes nombres.
 */
@Entity
@Table(name = "wishlists")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wishlist {

    /** Identificador único de la lista */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** UUID del usuario propietario */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Nombre de la lista — ej. "Cumpleaños", "Regalos" */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Si es true, cualquiera con el enlace puede ver la lista */
    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private boolean isPublic = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
