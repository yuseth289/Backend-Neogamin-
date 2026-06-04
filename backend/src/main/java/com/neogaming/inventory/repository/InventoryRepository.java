package com.neogaming.inventory.repository;

import com.neogaming.inventory.domain.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad Inventory.
 *
 * Incluye un método con bloqueo pesimista (PESSIMISTIC_WRITE) para
 * las operaciones de reserva de stock que deben ser atómicas y evitar
 * condiciones de carrera bajo alta concurrencia (dos checkouts simultáneos
 * para el último ítem disponible).
 */
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    /**
     * Busca el inventario de un producto específico.
     *
     * @param productId UUID del producto
     * @return El inventario si existe
     */
    Optional<Inventory> findByProductId(UUID productId);

    /**
     * Busca el inventario de un producto con bloqueo pesimista de escritura.
     *
     * El bloqueo PESSIMISTIC_WRITE asegura que ninguna otra transacción
     * pueda leer o escribir este registro hasta que la transacción actual
     * haga commit o rollback. Previene el overselling (vender más stock
     * del disponible) bajo alta concurrencia.
     *
     * Debe usarse dentro de una @Transactional para que el bloqueo funcione.
     *
     * @param productId UUID del producto
     * @return El inventario bloqueado para escritura
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByProductIdForUpdate(UUID productId);

    List<Inventory> findByProductIdIn(Collection<UUID> productIds);
}
