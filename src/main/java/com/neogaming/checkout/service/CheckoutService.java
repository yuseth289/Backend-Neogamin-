package com.neogaming.checkout.service;

import com.neogaming.address.repository.AddressRepository;
import com.neogaming.cart.domain.Cart;
import com.neogaming.cart.domain.CartItem;
import com.neogaming.cart.repository.CartItemRepository;
import com.neogaming.cart.repository.CartRepository;
import com.neogaming.cart.service.CartService;
import com.neogaming.catalog.product.domain.Product;
import com.neogaming.catalog.product.repository.ProductRepository;
import com.neogaming.checkout.domain.Checkout;
import com.neogaming.checkout.domain.CheckoutItem;
import com.neogaming.checkout.dto.request.InitCheckoutRequest;
import com.neogaming.checkout.dto.response.CheckoutResponse;
import com.neogaming.checkout.mapper.CheckoutMapper;
import com.neogaming.checkout.repository.CheckoutItemRepository;
import com.neogaming.checkout.repository.CheckoutRepository;
import com.neogaming.common.enums.EstadoCarrito;
import com.neogaming.common.enums.EstadoCheckout;
import com.neogaming.common.enums.EstadoGenerico;
import com.neogaming.common.exception.BusinessRuleException;
import com.neogaming.common.exception.ResourceNotFoundException;
import com.neogaming.inventory.service.InventoryService;
import com.neogaming.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio de gestión del proceso de checkout en NeoGaming.
 *
 * El checkout orquesta la transición del carrito a la orden:
 *  1. Valida que el carrito no esté vacío y todos los productos estén activos
 *  2. Verifica stock disponible para todos los ítems
 *  3. Reserva el stock de cada ítem (bloqueo pesimista en InventoryService)
 *  4. Captura snapshots de precios y dirección de entrega
 *  5. Convierte el carrito (status → CONVERTED)
 *  6. Crea el checkout con un tiempo de expiración
 *
 * Si algún paso falla, toda la transacción hace rollback y el stock
 * no queda reservado (garantía @Transactional).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CheckoutService {

    private final CheckoutRepository checkoutRepository;
    private final CheckoutItemRepository checkoutItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartService cartService;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final SellerRepository sellerRepository;
    private final InventoryService inventoryService;
    private final CheckoutMapper checkoutMapper;

    /** Minutos antes de que expire el checkout (configurable en application.yml) */
    @Value("${app.business.checkout-expiry-minutes:30}")
    private int checkoutExpiryMinutes;

    /**
     * Inicia el proceso de checkout a partir del carrito activo del usuario.
     *
     * Flujo completo:
     *  1. Verificar que no haya otro checkout PENDING activo
     *  2. Obtener el carrito activo del usuario
     *  3. Validar que el carrito no esté vacío
     *  4. Validar stock disponible para todos los ítems
     *  5. Reservar stock (bloqueo pesimista, atómico)
     *  6. Capturar dirección de entrega como snapshot
     *  7. Crear el checkout con sus ítems
     *  8. Marcar el carrito como CONVERTED
     *
     * @param request Dirección de entrega y método de pago
     * @param userId  UUID del usuario que inicia el checkout
     * @return El checkout creado con todos sus detalles
     * @throws BusinessRuleException si ya hay un checkout activo o el carrito está vacío
     */
    public CheckoutResponse iniciar(InitCheckoutRequest request, UUID userId) {
        // 1. Verificar que no haya otro checkout PENDING
        if (checkoutRepository.findByUserIdAndStatus(userId, EstadoCheckout.IN_PROGRESS).isPresent()) {
            throw new BusinessRuleException(
                    "Ya tienes un checkout activo. Complétalo o cancélalo antes de iniciar uno nuevo.",
                    "CHECKOUT_YA_ACTIVO"
            );
        }

        // 2. Obtener el carrito activo del usuario
        Cart cart = cartRepository.findByUserIdAndStatus(userId, EstadoCarrito.ACTIVE)
                .orElseThrow(() -> new BusinessRuleException(
                        "No tienes un carrito activo para iniciar el checkout",
                        "CARRITO_NO_ENCONTRADO"
                ));

        // 3. Obtener y validar los ítems del carrito
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BusinessRuleException(
                    "Tu carrito está vacío. Agrega productos antes de continuar.",
                    "CARRITO_VACIO"
            );
        }

        // 4. Obtener la dirección de entrega y construir el snapshot
        var address = addressRepository
                .findByIdAndUserIdAndStatus(request.addressId(), userId, EstadoGenerico.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección", request.addressId().toString()));

        Map<String, Object> shippingSnapshot = new HashMap<>();
        shippingSnapshot.put("id", address.getId());
        shippingSnapshot.put("label", address.getLabel());
        shippingSnapshot.put("street", address.getStreet());
        shippingSnapshot.put("number", address.getNumber());
        shippingSnapshot.put("floor", address.getFloor());
        shippingSnapshot.put("apartment", address.getApartment());
        shippingSnapshot.put("city", address.getCity());
        shippingSnapshot.put("department", address.getDepartment());
        shippingSnapshot.put("country", address.getCountry());
        shippingSnapshot.put("postalCode", address.getPostalCode());

        // 5. Validar productos activos, calcular precios y preparar ítems
        List<CheckoutItem> checkoutItems = new ArrayList<>();
        List<Map<String, Object>> itemsSnapshot = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new BusinessRuleException(
                            "Un producto en tu carrito ya no está disponible",
                            "PRODUCTO_NO_DISPONIBLE"
                    ));

            // Obtener el sellerId del producto para el split de pago
            UUID sellerId = sellerRepository.findByUserId(product.getSellerId())
                    .map(s -> s.getId())
                    .orElseThrow(() -> new BusinessRuleException(
                            "El vendedor de un producto no está disponible",
                            "VENDEDOR_NO_DISPONIBLE"
                    ));

            // Calcular precio final actual (puede diferir del precio en el carrito)
            BigDecimal precioFinal = calcularPrecioFinal(product);
            BigDecimal itemSubtotal = precioFinal.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            // 6. Reservar stock — usa bloqueo pesimista, puede lanzar BusinessRuleException
            inventoryService.reservarStock(cartItem.getProductId(), cartItem.getQuantity(), null);

            // Crear el ítem relacional del checkout
            CheckoutItem checkoutItem = CheckoutItem.builder()
                    .productId(cartItem.getProductId())
                    .sellerId(sellerId)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(precioFinal)
                    .subtotal(itemSubtotal)
                    .build();
            checkoutItems.add(checkoutItem);

            // Construir snapshot del ítem para el JSONB
            Map<String, Object> itemSnapshot = new HashMap<>();
            itemSnapshot.put("productId", cartItem.getProductId());
            itemSnapshot.put("productName", product.getName());
            itemSnapshot.put("sellerId", sellerId);
            itemSnapshot.put("quantity", cartItem.getQuantity());
            itemSnapshot.put("unitPrice", precioFinal);
            itemSnapshot.put("subtotal", itemSubtotal);
            itemsSnapshot.add(itemSnapshot);

            subtotal = subtotal.add(itemSubtotal);
        }

        // 7. Crear el checkout
        BigDecimal shippingCost = BigDecimal.ZERO;  // Envío gratuito por ahora
        BigDecimal total = subtotal.add(shippingCost);

        Checkout checkout = Checkout.builder()
                .userId(userId)
                .cartId(cart.getId())
                .status(EstadoCheckout.IN_PROGRESS)
                .itemsSnapshot(itemsSnapshot)
                .shippingAddress(shippingSnapshot)
                .subtotal(subtotal)
                .shippingCost(shippingCost)
                .total(total)
                .paymentMethod(request.paymentMethod())
                .expiresAt(Instant.now().plus(checkoutExpiryMinutes, ChronoUnit.MINUTES))
                .build();

        Checkout savedCheckout = checkoutRepository.save(checkout);

        // Guardar los ítems relacionales vinculando al checkout guardado
        checkoutItems.forEach(item -> {
            CheckoutItem itemConId = CheckoutItem.builder()
                    .checkoutId(savedCheckout.getId())
                    .productId(item.getProductId())
                    .sellerId(item.getSellerId())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .subtotal(item.getSubtotal())
                    .build();
            checkoutItemRepository.save(itemConId);
        });

        // 8. Marcar el carrito como CONVERTED
        cartService.marcarComoConvertido(cart.getId());

        // Construir respuesta con nombres de productos
        Map<UUID, String> productNames = new HashMap<>();
        cartItems.forEach(ci -> productRepository.findById(ci.getProductId())
                .ifPresent(p -> productNames.put(p.getId(), p.getName())));

        List<CheckoutItem> savedItems = checkoutItemRepository.findByCheckoutId(savedCheckout.getId());
        return checkoutMapper.toResponse(savedCheckout, savedItems, productNames);
    }

    /**
     * Obtiene el checkout activo (PENDING) del usuario.
     *
     * @param userId UUID del usuario
     * @return El checkout activo con sus detalles
     * @throws BusinessRuleException si no hay checkout activo
     */
    @Transactional(readOnly = true)
    public CheckoutResponse obtenerActivo(UUID userId) {
        Checkout checkout = checkoutRepository.findByUserIdAndStatus(userId, EstadoCheckout.IN_PROGRESS)
                .orElseThrow(() -> new BusinessRuleException(
                        "No tienes un checkout activo en este momento",
                        "CHECKOUT_NO_ENCONTRADO"
                ));

        if (checkout.haExpirado()) {
            throw new BusinessRuleException(
                    "Tu checkout expiró. Inicia uno nuevo desde tu carrito.",
                    "CHECKOUT_EXPIRADO"
            );
        }

        List<CheckoutItem> items = checkoutItemRepository.findByCheckoutId(checkout.getId());
        Map<UUID, String> productNames = construirMapaNombres(items);
        return checkoutMapper.toResponse(checkout, items, productNames);
    }

    /**
     * Cancela el checkout activo del usuario y libera el stock reservado.
     *
     * @param userId UUID del usuario
     */
    public void cancelar(UUID userId) {
        Checkout checkout = checkoutRepository.findByUserIdAndStatus(userId, EstadoCheckout.IN_PROGRESS)
                .orElseThrow(() -> new BusinessRuleException(
                        "No tienes un checkout activo para cancelar",
                        "CHECKOUT_NO_ENCONTRADO"
                ));

        // Liberar el stock reservado de cada ítem
        List<CheckoutItem> items = checkoutItemRepository.findByCheckoutId(checkout.getId());
        items.forEach(item ->
                inventoryService.liberarStock(item.getProductId(), item.getQuantity(), checkout.getId())
        );

        checkout.setStatus(EstadoCheckout.CANCELLED);
        checkoutRepository.save(checkout);
    }

    /**
     * Procesa los checkouts expirados liberando su stock reservado.
     * Llamado periódicamente por un @Scheduled job.
     * Marcado como público para que el scheduler pueda invocarlo.
     */
    public void procesarCheckoutsExpirados() {
        List<Checkout> expirados = checkoutRepository
                .findExpirados(EstadoCheckout.IN_PROGRESS, Instant.now());

        expirados.forEach(checkout -> {
            // Liberar stock de cada ítem del checkout expirado
            List<CheckoutItem> items = checkoutItemRepository.findByCheckoutId(checkout.getId());
            items.forEach(item ->
                    inventoryService.liberarStock(item.getProductId(), item.getQuantity(), checkout.getId())
            );
            checkout.setStatus(EstadoCheckout.EXPIRED);
            checkoutRepository.save(checkout);
        });
    }

    // ===== MÉTODOS AUXILIARES =====

    /**
     * Calcula el precio final con IVA de un producto.
     *
     * @param product Entidad del producto
     * @return Precio final con IVA en COP redondeado a 2 decimales
     */
    private BigDecimal calcularPrecioFinal(Product product) {
        BigDecimal factor = BigDecimal.ONE.add(
                product.getIvaPercent().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
        );
        return product.getBasePrice().multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Construye un mapa de productId → nombre del producto para el mapper.
     *
     * @param items Lista de ítems del checkout
     * @return Mapa con nombres de productos
     */
    private Map<UUID, String> construirMapaNombres(List<CheckoutItem> items) {
        Map<UUID, String> nombres = new HashMap<>();
        items.forEach(item ->
                productRepository.findById(item.getProductId())
                        .ifPresent(p -> nombres.put(p.getId(), p.getName()))
        );
        return nombres;
    }
}
