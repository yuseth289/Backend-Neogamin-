package com.neogaming.checkout.repository;

import com.neogaming.checkout.domain.Checkout;
import com.neogaming.common.enums.EstadoCheckout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad Checkout.
 */
public interface CheckoutRepository extends JpaRepository<Checkout, UUID> {

    /**
     * Busca el checkout activo (PENDING) de un usuario.
     * Un usuario solo puede tener un checkout pendiente a la vez.
     *
     * @param userId UUID del usuario
     * @param status Estado PENDING
     * @return El checkout activo del usuario si existe
     */
    Optional<Checkout> findByUserIdAndStatus(UUID userId, EstadoCheckout status);

    /**
     * Busca un checkout verificando que pertenezca al usuario.
     * Centraliza la validación de propiedad para operaciones del usuario.
     *
     * @param id     UUID del checkout
     * @param userId UUID del usuario
     * @return El checkout si existe y pertenece al usuario
     */
    Optional<Checkout> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Busca todos los checkouts PENDING que ya expiraron.
     * Usado por el batch job que libera stock de checkouts expirados.
     *
     * @param status   Estado PENDING
     * @param expiresAt Punto de corte (devuelve checkouts con expiresAt anterior a esto)
     * @return Lista de checkouts expirados que aún están en PENDING
     */
    @Query("SELECT c FROM Checkout c WHERE c.status = :status AND c.expiresAt < :expiresAt")
    List<Checkout> findExpirados(EstadoCheckout status, Instant expiresAt);
}
