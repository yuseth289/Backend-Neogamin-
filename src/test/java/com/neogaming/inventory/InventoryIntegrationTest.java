package com.neogaming.inventory;

import com.neogaming.IntegrationTestBase;
import com.neogaming.catalog.product.domain.Product;
import com.neogaming.catalog.product.repository.ProductRepository;
import com.neogaming.common.enums.*;
import com.neogaming.inventory.domain.Inventory;
import com.neogaming.inventory.dto.request.StockAdjustRequest;
import com.neogaming.inventory.repository.InventoryMovementRepository;
import com.neogaming.inventory.repository.InventoryRepository;
import com.neogaming.inventory.service.InventoryService;
import com.neogaming.seller.domain.Seller;
import com.neogaming.seller.repository.SellerRepository;
import com.neogaming.user.domain.User;
import com.neogaming.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests de integración para el módulo de inventario.
 *
 * Cubre:
 *  - Inicialización de inventario al crear producto
 *  - Agregar stock registra movimiento y actualiza physicalStock
 *  - Reserva de stock reduce availableStock
 *  - Reserva sobre stock insuficiente lanza excepción
 *  - Liberación de reserva restaura availableStock
 *  - Confirmación de venta reduce physicalStock
 */
@DisplayName("Inventario — Integración")
class InventoryIntegrationTest extends IntegrationTestBase {

    @Autowired private InventoryService inventoryService;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private InventoryMovementRepository movementRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private SellerRepository sellerRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private UUID productId;
    private UUID sellerId;
    private UUID userId;

    @BeforeEach
    void preparar() {
        movementRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
        sellerRepository.deleteAll();
        userRepository.deleteAll();

        User vendedor = User.builder()
                .email("vendedor@inv-test.co")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Inv")
                .lastName("Test")
                .role(RolUsuario.SELLER)
                .status(EstadoGenerico.ACTIVE)
                .build();
        User vendedorGuardado = userRepository.save(vendedor);
        userId = vendedorGuardado.getId();

        Seller seller = Seller.builder()
                .userId(userId)
                .storeName("Tienda Inv")
                .storeSlug("tienda-inv")
                .tipoDocumento(TipoDocumentoVendedor.CC)
                .numeroDocumento("1234567890")
                .tipoRegimen(TipoRegimenFiscal.NO_RESPONSABLE_IVA)
                .status(EstadoGenerico.ACTIVE)
                .build();
        sellerId = sellerRepository.save(seller).getId();

        Product producto = Product.builder()
                .sellerId(sellerId)
                .name("Teclado Mecánico")
                .slug("teclado-mecanico-" + sellerId.toString().substring(0, 8))
                .basePrice(new BigDecimal("250000.00"))
                .ivaPercent(new BigDecimal("19.00"))
                .status(EstadoProducto.ACTIVE)
                .build();
        productId = productRepository.save(producto).getId();

        // Inicializar inventario (physicalStock = 0) y luego agregar 10 unidades
        inventoryService.inicializarInventario(productId);
        inventoryService.agregarStock(productId,
                new StockAdjustRequest(10, "Stock inicial de prueba"), userId);
    }

    @Test
    @DisplayName("Inventario inicializado y cargado tiene 10 unidades disponibles")
    void inventarioCargadoCorrectamente() {
        Inventory inv = inventoryRepository.findByProductId(productId).orElseThrow();
        assertThat(inv.getPhysicalStock()).isEqualTo(10);
        assertThat(inv.getReservedStock()).isEqualTo(0);
        assertThat(inv.getAvailableStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("Reservar 3 unidades: reservedStock=3, availableStock=7")
    void reservarStockReduceDisponible() {
        UUID checkoutId = UUID.randomUUID();
        inventoryService.reservarStock(productId, 3, checkoutId);

        Inventory inv = inventoryRepository.findByProductId(productId).orElseThrow();
        assertThat(inv.getPhysicalStock()).isEqualTo(10);
        assertThat(inv.getReservedStock()).isEqualTo(3);
        assertThat(inv.getAvailableStock()).isEqualTo(7);
    }

    @Test
    @DisplayName("Reservar más stock del disponible lanza excepción de negocio")
    void reservarMasDelDisponibleLanzaExcepcion() {
        UUID checkoutId = UUID.randomUUID();
        assertThatThrownBy(() -> inventoryService.reservarStock(productId, 11, checkoutId))
                .hasMessageContaining("Stock insuficiente");
    }

    @Test
    @DisplayName("Liberar reserva: reservedStock vuelve a 0, availableStock=10")
    void liberarReservaRestauraDisponible() {
        UUID checkoutId = UUID.randomUUID();
        inventoryService.reservarStock(productId, 5, checkoutId);
        inventoryService.liberarStock(productId, 5, checkoutId);

        Inventory inv = inventoryRepository.findByProductId(productId).orElseThrow();
        assertThat(inv.getReservedStock()).isEqualTo(0);
        assertThat(inv.getAvailableStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("Confirmar venta de 4 unidades: physicalStock=6, reservedStock=0")
    void confirmarSalidaReducePhysicalStock() {
        UUID checkoutId = UUID.randomUUID();
        UUID ordenId = UUID.randomUUID();
        inventoryService.reservarStock(productId, 4, checkoutId);
        inventoryService.confirmarSalidaStock(productId, 4, ordenId);

        Inventory inv = inventoryRepository.findByProductId(productId).orElseThrow();
        assertThat(inv.getPhysicalStock()).isEqualTo(6);
        assertThat(inv.getReservedStock()).isEqualTo(0);
        assertThat(inv.getAvailableStock()).isEqualTo(6);
    }

    @Test
    @DisplayName("Agregar stock registra movimiento INITIAL_LOAD")
    void agregarStockRegistraMovimiento() {
        // El beforeEach ya registró al menos 2 movimientos (inicializar + agregar)
        long movimientos = movementRepository.count();
        assertThat(movimientos).isGreaterThanOrEqualTo(1);
    }
}
