package com.neogaming.auth.repository;

import com.neogaming.auth.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de acceso a datos para la entidad Session.
 *
 * Provee operaciones de búsqueda por hash de token (para el flujo de refresh)
 * y limpieza de sesiones expiradas (mantenimiento periódico).
 */
public interface SessionRepository extends JpaRepository<Session, UUID> {

    /**
     * Busca una sesión por el hash del refresh token.
     * Se usa en el flujo de renovación de tokens (POST /auth/refresh).
     *
     * @param tokenHash Hash SHA-256 del refresh token recibido del cliente
     * @return Sesión si existe (puede estar revocada o expirada, validar con isActive())
     */
    Optional<Session> findByTokenHash(String tokenHash);

    /**
     * Elimina todas las sesiones expiradas antes de una fecha dada.
     * Se puede usar en un job de limpieza periódico para mantener la tabla ligera.
     *
     * @param before Fecha límite; se eliminarán sesiones cuyo expiresAt sea anterior a esta fecha
     */
    @Modifying
    @Query("DELETE FROM Session s WHERE s.expiresAt < :before")
    void deleteExpiredSessionsBefore(@Param("before") Instant before);
}
