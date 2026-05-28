package com.neogaming.inventory.service;

import com.neogaming.catalog.product.repository.ProductRepository;
import com.neogaming.common.enums.TipoMovimientoStock;
import com.neogaming.common.exception.BusinessRuleException;
import com.neogaming.common.exception.ResourceNotFoundException;
import com.neogaming.common.response.PageResponse;
import com.neogaming.inventory.domain.Inventory;
import com.neogaming.inventory.domain.InventoryMovement;
import com.neogaming.inventory.dto.request.StockAdjustRequest;
import com.neogaming.inventory.dto.response.InventoryMovementResponse;
import com.neogaming.inventory.dto.response.InventoryResponse;
import com.neogaming.inventory.mapper.InventoryMapper;
import com.neogaming.inventory.repository.InventoryMovementRepository;
import com.neogaming.inventory.repository.InventoryRepository;
import com.neogaming.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servicio de gestión de inventario para NeoGaming.
 *
 * Reglas de negocio implementadas:
 *  - El inventario se crea automáticamente al crear un producto (stock inicial = 0)
 *  - Solo el vendedor propietario puede ajustar su stock
 *  - La reserva de stock usa bloqueo pesimista (PESSIMISTIC_WRITE) para evitar
 *    condiciones de carrera (dos checkouts simultáneos para el último ítem)
 *  - Todos los cambios quedan registrados en inventory_movements (log inmutable)
 *  - El availableStock = physicalStock - reservedStock nunca se persiste
 *
 * Los métodos reservarStock() y liberarStock() son usados internamente
 * por el módulo de checkout y NO tienen endpoint REST directo.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;
    private final InventoryMapper inventoryMapper;

    /**
     * Crea el registro de inventario para un producto recién creado.
     * Se llama internamente desde ProductService al crear un producto.
     * El stock inicial es siempre 0 — el vendedor lo ajusta por separado.
     *
     * @param productId UUID del producto recién creado
     */
    public void inicializarInventario(UUID productId) {
        Inventory inventory = Inventory.builder()
                .productId(productId)
                .physicalStock(0)
                .reservedStock(0)
                .build();
        inventoryRepository.save(inventory);
    }

    /**
     * Obtiene el estado actual del inventario de un producto.
     *
     * @param productId UUID del producto
     * @return Estado actual del inventario con availableStock calculado
     * @throws ResourceNotFoundException si el producto no tiene inventario
     */
    @Transactional(readOnly = true)
    public InventoryResponse obtenerPorProducto(UUID productId) {
        Inventory inventory = buscarPorProducto(productId);
        return inventoryMapper.toResponse(inventory);
    }

    /**
     * Agrega stock físico al inventario de un producto (entrada de mercancía).
     *
     * Solo puede hacerlo el vendedor propietario del producto.
     * Registra un movimiento de tipo IN en el historial.
     *
     * @param productId UUID del producto
     * @param request   Cantidad a agregar y notas del movimiento
     * @param userId    UUID del usuario vendedor (para validar propiedad)
     * @return Estado actualizado del inventario
     */
    public InventoryResponse agregarStock(UUID productId, StockAdjustRequest request, UUID userId) {
        validarPropiedadDelProducto(productId, userId);

        // Bloqueo pesimista para evitar condiciones de carrera
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventario", productId.toString()));

        int cantidadAnteriorFisica = inventory.getPhysicalStock();
        inventory.setPhysicalStock(cantidadAnteriorFisica + request.quantity());
        inventoryRepository.save(inventory);

        // Registrar el movimiento en el historial
        registrarMovimiento(
                inventory, TipoMovimientoStock.INITIAL_LOAD, request.quantity(),
                null, request.notes() != null ? request.notes() : "Entrada de stock"
        );

        return inventoryMapper.toResponse(inventory);
    }

    /**
     * Ajusta el stock físico a un valor absoluto (corrección manual).
     *
     * Se usa cuando el vendedor hace un conteo físico y corrige el sistema.
     * No puede dejar el physicalStock por debajo del reservedStock actual.
     *
     * @param productId UUID del producto
     * @param request   Nuevo valor de stock y motivo del ajuste
     * @param userId    UUID del usuario vendedor
     * @return Estado actualizado del inventario
     */
    public InventoryResponse ajustarStock(UUID productId, StockAdjustRequest request, UUID userId) {
        validarPropiedadDelProducto(productId, userId);

        Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventario", productId.toString()));

        // No se puede ajustar a menos del stock reservado
        if (request.quantity() < inventory.getReservedStock()) {
            throw new BusinessRuleException(
                    "No se puede ajustar el stock a " + request.quantity()
                            + " porque hay " + inventory.getReservedStock()
                            + " unidades reservadas por checkouts activos",
                    "AJUSTE_MENOR_QUE_RESERVADO"
            );
        }

        inventory.setPhysicalStock(request.quantity());
        inventoryRepository.save(inventory);

        registrarMovimiento(
                inventory, TipoMovimientoStock.MANUAL_ADJUSTMENT, request.quantity(),
                null, request.notes() != null ? request.notes() : "Ajuste manual de inventario"
        );

        return inventoryMapper.toResponse(inventory);
    }

    /**
     * Reserva stock para un checkout pendiente de pago.
     *
     * Usa bloqueo pesimista (PESSIMISTIC_WRITE) para garantizar atomicidad.
     * Si no hay suficiente stock disponible, lanza excepción y el checkout
     * no se crea.
     *
     * MÉTODO INTERNO — llamado por CheckoutService, sin endpoint REST directo.
     *
     * @param productId   UUID del producto
     * @param cantidad    Unidades a reservar
     * @param checkoutId  UUID del checkout que origina la reserva (para auditoría)
     * @throws BusinessRuleException si no hay suficiente stock disponible
     */
    public void reservarStock(UUID productId, int cantidad, UUID checkoutId) {
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventario", productId.toString()));

        if (!inventory.tieneStockDisponible(cantidad)) {
            throw new BusinessRuleException(
                    "Stock insuficiente. Disponible: " + inventory.getAvailableStock()
                            + ", solicitado: " + cantidad,
                    "STOCK_INSUFICIENTE"
            );
        }

        inventory.setReservedStock(inventory.getReservedStock() + cantidad);
        inventoryRepository.save(inventory);

        registrarMovimiento(inventory, TipoMovimientoStock.PURCHASE_RESERVE, cantidad, checkoutId, "Reserva por checkout");
    }

    /**
     * Libera stock reservado cuando un checkout expira o el pago falla.
     *
     * MÉTODO INTERNO — llamado por CheckoutService y PaymentService.
     *
     * @param productId  UUID del producto
     * @param cantidad   Unidades a liberar
     * @param referenceId UUID del checkout u orden relacionado
     */
    public void liberarStock(UUID productId, int cantidad, UUID referenceId) {
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventario", productId.toString()));

        int nuevaReserva = Math.max(0, inventory.getReservedStock() - cantidad);
        inventory.setReservedStock(nuevaReserva);
        inventoryRepository.save(inventory);

        registrarMovimiento(inventory, TipoMovimientoStock.RESERVE_RELEASE, cantidad, referenceId, "Liberación de reserva");
    }

    /**
     * Confirma la salida de stock al confirmar una orden pagada.
     * Reduce tanto el physicalStock como el reservedStock en la cantidad vendida.
     *
     * MÉTODO INTERNO — llamado por PaymentService al confirmar el pago.
     *
     * @param productId UUID del producto
     * @param cantidad  Unidades vendidas
     * @param ordenId   UUID de la orden confirmada
     */
    public void confirmarSalidaStock(UUID productId, int cantidad, UUID ordenId) {
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventario", productId.toString()));

        inventory.setPhysicalStock(inventory.getPhysicalStock() - cantidad);
        inventory.setReservedStock(Math.max(0, inventory.getReservedStock() - cantidad));
        inventoryRepository.save(inventory);

        registrarMovimiento(inventory, TipoMovimientoStock.SALE_CONFIRMED, cantidad, ordenId, "Salida por orden confirmada");
    }

    /**
     * Lista el historial de movimientos de inventario de un producto.
     * Paginado y ordenado del más reciente al más antiguo.
     *
     * @param productId UUID del producto
     * @param userId    UUID del vendedor (para validar propiedad)
     * @param pageable  Paginación
     * @return Página de movimientos del inventario
     */
    @Transactional(readOnly = true)
    public PageResponse<InventoryMovementResponse> listarMovimientos(
            UUID productId, UUID userId, Pageable pageable) {
        validarPropiedadDelProducto(productId, userId);

        Inventory inventory = buscarPorProducto(productId);

        return PageResponse.from(
                movementRepository
                        .findByInventoryIdOrderByCreatedAtDesc(inventory.getId(), pageable)
                        .map(inventoryMapper::toMovementResponse)
        );
    }

    // ===== MÉTODOS AUXILIARES =====

    /**
     * Verifica que el producto pertenece al vendedor autenticado.
     *
     * @param productId UUID del producto
     * @param userId    UUID del usuario vendedor
     * @throws BusinessRuleException si el producto no pertenece al vendedor
     */
    private void validarPropiedadDelProducto(UUID productId, UUID userId) {
        sellerRepository.findByUserId(userId)
                .map(seller -> productRepository.findByIdAndSellerId(productId, seller.getId()))
                .filter(opt -> opt.isPresent())
                .orElseThrow(() -> new BusinessRuleException(
                        "No tienes permiso para gestionar el inventario de este producto",
                        "ACCESO_DENEGADO_INVENTARIO"
                ));
    }

    /**
     * Busca el inventario de un producto o lanza excepción si no existe.
     *
     * @param productId UUID del producto
     * @return La entidad Inventory del producto
     */
    private Inventory buscarPorProducto(UUID productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventario del producto", productId.toString()));
    }

    /**
     * Registra un movimiento en el historial de inventario.
     * Captura el estado del stock después del movimiento para auditoría.
     *
     * @param inventory    Inventario ya actualizado (con los valores post-movimiento)
     * @param tipo         Tipo de movimiento
     * @param cantidad     Unidades del movimiento
     * @param referenceId  UUID del documento relacionado (puede ser null)
     * @param notes        Descripción del movimiento
     */
    @Transactional(readOnly = true)
    public Integer obtenerStockDisponible(UUID productId) {
        return inventoryRepository.findByProductId(productId)
                .map(inv -> inv.getPhysicalStock() - inv.getReservedStock())
                .orElse(null);
    }

    private void registrarMovimiento(Inventory inventory, TipoMovimientoStock tipo,
                                     int cantidad, UUID referenceId, String notes) {
        InventoryMovement movement = InventoryMovement.builder()
                .inventoryId(inventory.getId())
                .productId(inventory.getProductId())
                .movementType(tipo)
                .quantity(cantidad)
                .physicalAfter(inventory.getPhysicalStock())
                .reservedAfter(inventory.getReservedStock())
                .referenceId(referenceId)
                .notes(notes)
                .build();
        movementRepository.save(movement);
    }
}
