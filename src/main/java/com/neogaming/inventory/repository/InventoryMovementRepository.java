package com.neogaming.inventory.repository;

import com.neogaming.inventory.domain.InventoryMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositorio JPA para el historial de movimientos de inventario.
 * Solo se leen movimientos — nunca se editan ni eliminan (log inmutable).
 */
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {

    /**
     * Lista el historial de movimientos de un inventario con paginación.
     * Ordenado por fecha descendente (más reciente primero).
     *
     * @param inventoryId UUID del inventario
     * @param pageable    Paginación
     * @return Página de movimientos del inventario
     */
    Page<InventoryMovement> findByInventoryIdOrderByCreatedAtDesc(UUID inventoryId, Pageable pageable);
}
