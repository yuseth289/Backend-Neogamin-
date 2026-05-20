package com.neogaming.catalog.offer.repository;

import com.neogaming.catalog.offer.domain.Offer;
import com.neogaming.common.enums.EstadoGenerico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad Offer.
 */
public interface OfferRepository extends JpaRepository<Offer, UUID> {

    /**
     * Lista todas las ofertas de un producto.
     *
     * @param productId UUID del producto
     * @return Lista de ofertas del producto
     */
    List<Offer> findByProductIdOrderByCreatedAtDesc(UUID productId);

    /**
     * Busca la oferta vigente actual de un producto.
     * Una oferta está vigente si: status=ACTIVE, ahora >= startDate, ahora <= endDate.
     *
     * @param productId UUID del producto
     * @param status    Estado a verificar (ACTIVE)
     * @param ahora     Momento actual para verificar vigencia
     * @return La oferta vigente si existe
     */
    @Query("""
            SELECT o FROM Offer o
            WHERE o.productId = :productId
            AND o.status = :status
            AND o.startDate <= :ahora
            AND o.endDate >= :ahora
            """)
    Optional<Offer> findOfertaVigente(UUID productId, EstadoGenerico status, Instant ahora);

    /**
     * Verifica si ya existe una oferta activa para el producto en el período dado.
     * Previene crear ofertas que se solapan con la oferta actual activa.
     *
     * @param productId UUID del producto
     * @param status    Estado ACTIVE
     * @param startDate Inicio de la nueva oferta
     * @param endDate   Fin de la nueva oferta
     * @return true si ya hay una oferta que se solapa
     */
    @Query("""
            SELECT COUNT(o) > 0 FROM Offer o
            WHERE o.productId = :productId
            AND o.status = :status
            AND o.startDate < :endDate
            AND o.endDate > :startDate
            """)
    boolean existeSolapamiento(UUID productId, EstadoGenerico status,
                               Instant startDate, Instant endDate);
}
