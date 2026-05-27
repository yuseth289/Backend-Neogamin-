package com.neogaming.inventory.mapper;

import com.neogaming.inventory.domain.Inventory;
import com.neogaming.inventory.domain.InventoryMovement;
import com.neogaming.inventory.dto.response.InventoryMovementResponse;
import com.neogaming.inventory.dto.response.InventoryResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entidades de inventario a sus DTOs de respuesta.
 */
@Component
public class InventoryMapper {

    /**
     * Convierte un Inventory a su DTO de respuesta.
     * Calcula el availableStock = physicalStock - reservedStock.
     *
     * @param inventory Entidad del inventario
     * @return DTO con los tres valores de stock
     */
    public InventoryResponse toResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getPhysicalStock(),
                inventory.getReservedStock(),
                inventory.getAvailableStock(),
                inventory.getUpdatedAt()
        );
    }

    /**
     * Convierte un InventoryMovement a su DTO de respuesta.
     *
     * @param movement Entidad del movimiento de inventario
     * @return DTO del movimiento
     */
    public InventoryMovementResponse toMovementResponse(InventoryMovement movement) {
        return new InventoryMovementResponse(
                movement.getId(),
                movement.getMovementType(),
                movement.getQuantity(),
                movement.getPhysicalAfter(),
                movement.getReservedAfter(),
                movement.getReferenceId(),
                movement.getNotes(),
                movement.getCreatedAt()
        );
    }
}
