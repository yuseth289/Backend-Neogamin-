package com.neogaming.cart.domain;

import com.neogaming.common.audit.AuditableEntity;
import com.neogaming.common.enums.EstadoCarrito;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entidad que representa el carrito de compras de un usuario.
 *
 * Un usuario tiene exactamente un carrito ACTIVE a la vez.
 * El carrito no reserva stock — la reserva ocurre al crear el checkout.
 *
 * Ciclo de vida:
 *  ACTIVE    → usuario está agregando productos
 *  CONVERTED → el usuario inició el checkout (el carrito queda registrado)
 *  ABANDONED → inactivo por mucho tiempo sin convertirse (cleanup batch)
 */
@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart extends AuditableEntity {

    /** Identificador único del carrito */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** UUID del usuario propietario del carrito */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Estado del carrito.
     * ACTIVE: carrito actual del usuario.
     * CONVERTED: convertido en checkout (historial).
     * ABANDONED: marcado como abandonado por cleanup.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EstadoCarrito status = EstadoCarrito.ACTIVE;
}
