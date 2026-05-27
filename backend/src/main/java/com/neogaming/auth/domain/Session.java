package com.neogaming.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad que representa una sesión activa de usuario en NeoGaming.
 *
 * En lugar de un sistema stateless puro, NeoGaming usa una tabla de sesiones
 * para poder revocar tokens JWT antes de que expiren (logout real).
 *
 * Mecánica:
 *  - Al hacer login, se crea una Session y su ID se embebe en el JWT como claim "sessionId".
 *  - En cada request, el JwtAuthenticationFilter busca la sesión por su ID.
 *  - Si la sesión está revocada (revokedAt != null), el request es rechazado (401).
 *  - Al hacer logout, se registra la fecha de revocación en revokedAt.
 *  - Al usar el refresh token, la sesión antigua se revoca y se crea una nueva.
 *
 * Seguridad del refresh token:
 *  - El refresh token es un UUID aleatorio que se envía al cliente.
 *  - En la base de datos solo se almacena el hash SHA-256 del token (tokenHash).
 *  - Esto evita que una filtración de la BD comprometa los refresh tokens.
 */
@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    /** ID del usuario dueño de esta sesión */
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    /**
     * Hash SHA-256 del refresh token enviado al cliente.
     * Nunca se almacena el token en texto plano.
     */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    /** Información del dispositivo o cliente (User-Agent del request de login) */
    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    /** Fecha de expiración del refresh token (30 días desde la creación) */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Fecha de revocación de la sesión.
     * null    = sesión activa
     * non-null = sesión revocada (logout o rotación de token)
     */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** Fecha de creación. Se asigna manualmente en el @PrePersist (Session no extiende AuditableEntity) */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Se ejecuta automáticamente antes de la primera persistencia.
     * Asigna la fecha de creación si no fue asignada manualmente.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Verifica si la sesión está activa (no revocada y no expirada).
     *
     * @return true si la sesión puede ser usada para autenticar requests
     */
    public boolean isActive() {
        return revokedAt == null && Instant.now().isBefore(expiresAt);
    }
}
