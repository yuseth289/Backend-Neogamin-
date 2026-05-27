package com.neogaming.invoice.repository;

import com.neogaming.common.enums.EstadoFactura;
import com.neogaming.invoice.domain.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad Invoice.
 */
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    /** Busca la factura de una orden específica. */
    Optional<Invoice> findByOrderId(UUID orderId);

    /** Verifica si ya existe una factura para la orden dada. */
    boolean existsByOrderId(UUID orderId);

    /** Lista todas las facturas de un usuario paginadas. */
    Page<Invoice> findByUserId(UUID userId, Pageable pageable);

    /** Lista facturas por estado, usada para trabajos de reintento de envío. */
    Page<Invoice> findByStatus(EstadoFactura status, Pageable pageable);

    /**
     * Genera el siguiente número de secuencia para el año indicado.
     * Cuenta cuántas facturas ya existen en ese año y retorna count + 1.
     */
    @Query("SELECT COUNT(i) + 1 FROM Invoice i WHERE YEAR(i.createdAt) = :year")
    long nextSequenceForYear(int year);
}
