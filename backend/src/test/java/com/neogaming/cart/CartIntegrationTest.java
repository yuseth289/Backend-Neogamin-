package com.neogaming.cart;

import com.neogaming.IntegrationTestBase;
import com.neogaming.cart.dto.request.AddCartItemRequest;
import com.neogaming.cart.repository.CartItemRepository;
import com.neogaming.cart.repository.CartRepository;
import com.neogaming.catalog.product.domain.Product;
import com.neogaming.catalog.product.repository.ProductRepository;
import com.neogaming.common.enums.*;
import com.neogaming.inventory.domain.Inventory;
import com.neogaming.inventory.repository.InventoryRepository;
import com.neogaming.seller.domain.Seller;
import com.neogaming.seller.repository.SellerRepository;
import com.neogaming.user.domain.User;
import com.neogaming.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integración para el módulo del carrito de compras.
 *
 * Cubre:
 *  - Obtener carrito vacío de un usuario nuevo
 *  - Agregar producto al carrito
 *  - Incrementar cantidad al agregar el mismo producto
 *  - Eliminar ítem del carrito
 */
@DisplayName("Carrito — Integración")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CartIntegrationTest extends IntegrationTestBase {

    @Autowired private UserRepository userRepository;
    @Autowired private SellerRepository sellerRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String accessToken;
    private UUID productId;

    @BeforeEach
    void preparar() {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
        sellerRepository.deleteAll();
        userRepository.deleteAll();

        // Crear usuario comprador
        User comprador = User.builder()
                .email("comprador@test.co")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Juan")
                .lastName("Comprador")
                .role(RolUsuario.CLIENT)
                .status(EstadoGenerico.ACTIVE)
                .build();
        userRepository.save(comprador);

        // Crear usuario vendedor
        User usuarioVendedor = User.builder()
                .email("vendedor@test.co")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Pedro")
                .lastName("Vendedor")
                .role(RolUsuario.SELLER)
                .status(EstadoGenerico.ACTIVE)
                .build();
        User vendedorGuardado = userRepository.save(usuarioVendedor);

        Seller seller = Seller.builder()
                .userId(vendedorGuardado.getId())
                .storeName("Tienda Test")
                .storeSlug("tienda-test")
                .tipoDocumento(TipoDocumentoVendedor.NIT)
                .numeroDocumento("900123456")
                .tipoRegimen(TipoRegimenFiscal.RESPONSABLE_IVA)
                .status(EstadoGenerico.ACTIVE)
                .build();
        Seller sellerGuardado = sellerRepository.save(seller);

        // Crear producto activo
        Product producto = Product.builder()
                .sellerId(sellerGuardado.getId())
                .name("Control PS5")
                .slug("control-ps5-" + sellerGuardado.getId().toString().substring(0, 8))
                .basePrice(new BigDecimal("320000.00"))
                .ivaPercent(new BigDecimal("19.00"))
                .status(EstadoProducto.ACTIVE)
                .build();
        Product productoGuardado = productRepository.save(producto);
        productId = productoGuardado.getId();

        // Crear inventario con stock disponible
        Inventory inventario = Inventory.builder()
                .productId(productId)
                .physicalStock(50)
                .reservedStock(0)
                .build();
        inventoryRepository.save(inventario);

        // Obtener access token vía login
        Map<String, String> loginReq = Map.of(
                "email", "comprador@test.co", "password", "Password123!");
        ResponseEntity<Map<String, Object>> loginResp = restTemplate.exchange(
                "http://localhost:" + port + "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginReq, jsonHeaders()),
                mapType());
        Map<String, Object> data = extraerData(loginResp);
        accessToken = (String) data.get("accessToken");
    }

    @Test
    @DisplayName("GET /cart — carrito vacío de usuario nuevo")
    void carritoVacioUsuarioNuevo() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "http://localhost:" + port + "/cart",
                HttpMethod.GET,
                authEntity(),
                mapType());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = extraerData(response);
        assertThat(data).containsKey("items");
    }

    @Test
    @DisplayName("POST /cart/items — agrega producto al carrito")
    void agregarProductoAlCarrito() {
        AddCartItemRequest req = new AddCartItemRequest(productId, 2);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "http://localhost:" + port + "/cart/items",
                HttpMethod.POST,
                new HttpEntity<>(req, authHeaders()),
                mapType());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> items = (List<?>) extraerData(response).get("items");
        assertThat(items).hasSize(1);
    }

    @Test
    @DisplayName("POST /cart/items — agregar mismo producto incrementa cantidad")
    void agregarMismoProductoIncrementaCantidad() {
        AddCartItemRequest req = new AddCartItemRequest(productId, 1);

        restTemplate.exchange("http://localhost:" + port + "/cart/items",
                HttpMethod.POST, new HttpEntity<>(req, authHeaders()), mapType());
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "http://localhost:" + port + "/cart/items",
                HttpMethod.POST, new HttpEntity<>(req, authHeaders()), mapType());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) extraerData(response).get("items");
        assertThat(items).hasSize(1);
        assertThat(((Number) items.get(0).get("quantity")).intValue()).isEqualTo(2);
    }

    @Test
    @DisplayName("DELETE /cart/items/{productId} — elimina ítem del carrito")
    void eliminarItemDelCarrito() {
        AddCartItemRequest req = new AddCartItemRequest(productId, 1);
        restTemplate.exchange("http://localhost:" + port + "/cart/items",
                HttpMethod.POST, new HttpEntity<>(req, authHeaders()), mapType());

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "http://localhost:" + port + "/cart/items/" + productId,
                HttpMethod.DELETE, authEntity(), mapType());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map<String, Object>> cartResp = restTemplate.exchange(
                "http://localhost:" + port + "/cart",
                HttpMethod.GET, authEntity(), mapType());
        List<?> items = (List<?>) extraerData(cartResp).get("items");
        assertThat(items).isEmpty();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static final Class<Map<String, Object>> MAP_CLASS =
            (Class<Map<String, Object>>) (Class<?>) Map.class;

    private Class<Map<String, Object>> mapType() {
        return MAP_CLASS;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private HttpHeaders authHeaders() {
        HttpHeaders h = jsonHeaders();
        h.setBearerAuth(accessToken);
        return h;
    }

    private HttpEntity<Void> authEntity() {
        return new HttpEntity<>(authHeaders());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extraerData(ResponseEntity<Map<String, Object>> resp) {
        Map<String, Object> body = resp.getBody();
        assertThat(body).isNotNull();
        return (Map<String, Object>) body.get("data");
    }
}
