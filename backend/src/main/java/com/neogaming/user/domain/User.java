package com.neogaming.user.domain;

import com.neogaming.common.audit.AuditableEntity;
import com.neogaming.common.enums.EstadoGenerico;
import com.neogaming.common.enums.RolUsuario;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entidad principal que representa a un usuario registrado en NeoGaming.
 *
 * Un usuario puede tener tres roles:
 *  - CLIENT : comprador, puede agregar productos al carrito, hacer pedidos, etc.
 *  - SELLER : vendedor, además tiene un perfil de tienda (entidad Seller)
 *  - ADMIN  : administrador, acceso total a la plataforma
 *
 * El campo role puede cambiar de CLIENT a SELLER cuando el usuario aplica
 * para ser vendedor y el administrador aprueba su solicitud (módulo seller).
 *
 * La contraseña nunca se almacena en texto plano — solo el hash BCrypt en passwordHash.
 * Los campos createdAt y updatedAt son manejados automáticamente por AuditableEntity.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends AuditableEntity {

    /** Identificador único del usuario. Generado automáticamente por JPA (UUID v4). */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    /** Email del usuario. Único en la plataforma, se usa como identificador de login. */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Hash BCrypt de la contraseña del usuario.
     * Nunca exponer este campo en ningún DTO de respuesta.
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /** Teléfono de contacto. Opcional. */
    @Column(length = 20)
    private String phone;

    /** URL o base64 de la imagen de perfil. */
    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    /**
     * Rol del usuario en la plataforma.
     * Almacenado como String para mayor legibilidad en la BD.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RolUsuario role;

    /**
     * Estado de la cuenta del usuario.
     * ACTIVE   → acceso normal
     * INACTIVE → requiere activación
     * SUSPENDED → bloqueado por administrador
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoGenerico status;

    /**
     * Indica si el usuario verificó su email.
     * En esta versión el registro es inmediato, pero el campo
     * existe para implementar verificación futura.
     */
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;
}
