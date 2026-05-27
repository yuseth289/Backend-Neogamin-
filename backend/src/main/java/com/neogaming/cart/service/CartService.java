package com.neogaming.cart.service;

import com.neogaming.cart.domain.Cart;
import com.neogaming.cart.domain.CartItem;
import com.neogaming.cart.dto.request.AddCartItemRequest;
import com.neogaming.cart.dto.response.CartItemResponse;
import com.neogaming.cart.dto.response.CartResponse;
import com.neogaming.cart.repository.CartItemRepository;
import com.neogaming.cart.repository.CartRepository;
import com.neogaming.catalog.product.domain.Product;
import com.neogaming.catalog.product.repository.ProductImageRepository;
import com.neogaming.catalog.product.repository.ProductRepository;
import com.neogaming.common.enums.EstadoCarrito;
import com.neogaming.common.enums.EstadoProducto;
import com.neogaming.common.exception.BusinessRuleException;
import com.neogaming.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de gestión del carrito de compras para NeoGaming.
 *
 * Reglas de negocio implementadas:
 *  - Un usuario tiene exactamente un carrito ACTIVE a la vez
 *  - Si no tiene carrito, se crea automáticamente al agregar el primer ítem
 *  - Solo productos ACTIVE pueden agregarse al carrito
 *  - El carrito no reserva stock — la reserva ocurre al crear el checkout
 *  - Si el usuario agrega un producto ya en el carrito, se suma la cantidad
 *  - Al obtener el carrito, se detectan cambios de precio desde que se agregó
 *  - El subtotal y total se calculan en tiempo real (no se persisten)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    /**
     * Obtiene el carrito activo del usuario con todos sus ítems.
     * Si el usuario no tiene carrito, retorna un carrito vacío virtual
     * (sin crear uno en la BD hasta que se agregue algún ítem).
     *
     * Al cargar los ítems, compara el precio guardado con el precio actual
     * para detectar cambios de precio.
     *
     * @param userId UUID del usuario
     * @return El carrito activo con sus ítems y totales calculados
     */
    @Transactional(readOnly = true)
    public CartResponse obtenerCarrito(UUID userId) {
        return cartRepository.findByUserIdAndStatus(userId, EstadoCarrito.ACTIVE)
                .map(cart -> construirCartResponse(cart))
                .orElseGet(() -> new CartResponse(null, List.of(), BigDecimal.ZERO, 0, false));
    }

    /**
     * Agrega un producto al carrito del usuario.
     *
     * Si el producto ya está en el carrito, incrementa la cantidad.
     * Si el usuario no tiene carrito, lo crea automáticamente.
     * Valida que el producto esté activo (no verifica stock — eso lo hace checkout).
     *
     * @param request Producto a agregar y cantidad
     * @param userId  UUID del usuario
     * @return El carrito actualizado
     * @throws BusinessRuleException si el producto no está activo
     */
    public CartResponse agregar(AddCartItemRequest request, UUID userId) {
        Product product = productRepository.findById(request.productId())
                .filter(p -> p.getStatus() == EstadoProducto.ACTIVE)
                .orElseThrow(() -> new BusinessRuleException(
                        "El producto no está disponible para agregar al carrito",
                        "PRODUCTO_NO_DISPONIBLE"
                ));

        Cart cart = obtenerOCrearCarrito(userId);

        // Si ya existe el ítem, incrementar cantidad; si no, crearlo
        cartItemRepository.findByCartIdAndProductId(cart.getId(), request.productId())
                .ifPresentOrElse(
                        item -> {
                            item.setQuantity(item.getQuantity() + request.quantity());
                            cartItemRepository.save(item);
                        },
                        () -> {
                            BigDecimal precioFinal = calcularPrecioFinal(product);
                            CartItem nuevoItem = CartItem.builder()
                                    .cartId(cart.getId())
                                    .productId(request.productId())
                                    .quantity(request.quantity())
                                    .unitPrice(precioFinal)
                                    .build();
                            cartItemRepository.save(nuevoItem);
                        }
                );

        return construirCartResponse(cart);
    }

    /**
     * Actualiza la cantidad de un ítem en el carrito.
     * Si la nueva cantidad es 0, elimina el ítem del carrito.
     *
     * @param itemId   UUID del ítem a actualizar
     * @param cantidad Nueva cantidad (0 = eliminar ítem)
     * @param userId   UUID del usuario (para validar que el ítem le pertenece)
     * @return El carrito actualizado
     */
    public CartResponse actualizarCantidad(UUID itemId, int cantidad, UUID userId) {
        Cart cart = obtenerCarritoActivo(userId);

        CartItem item = cartItemRepository.findById(itemId)
                .filter(i -> i.getCartId().equals(cart.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Ítem del carrito", itemId.toString()));

        if (cantidad <= 0) {
            // Cantidad 0 o negativa: eliminar el ítem
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(cantidad);
            cartItemRepository.save(item);
        }

        return construirCartResponse(cart);
    }

    /**
     * Elimina un ítem específico del carrito.
     *
     * @param itemId UUID del ítem a eliminar
     * @param userId UUID del usuario
     * @return El carrito actualizado
     */
    public CartResponse eliminarItem(UUID itemId, UUID userId) {
        Cart cart = obtenerCarritoActivo(userId);

        CartItem item = cartItemRepository.findById(itemId)
                .filter(i -> i.getCartId().equals(cart.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Ítem del carrito", itemId.toString()));

        cartItemRepository.delete(item);
        return construirCartResponse(cart);
    }

    /**
     * Vacía completamente el carrito del usuario eliminando todos sus ítems.
     * El carrito sigue existiendo (en estado ACTIVE) pero sin ítems.
     *
     * @param userId UUID del usuario
     */
    public void vaciar(UUID userId) {
        cartRepository.findByUserIdAndStatus(userId, EstadoCarrito.ACTIVE)
                .ifPresent(cart -> cartItemRepository.deleteByCartId(cart.getId()));
    }

    /**
     * Marca el carrito como CONVERTED al iniciar el checkout.
     * Llamado internamente por CheckoutService.
     *
     * @param cartId UUID del carrito
     */
    public void marcarComoConvertido(UUID cartId) {
        cartRepository.findById(cartId).ifPresent(cart -> {
            cart.setStatus(EstadoCarrito.CONVERTED);
            cartRepository.save(cart);
        });
    }

    // ===== MÉTODOS AUXILIARES =====

    /**
     * Obtiene el carrito activo del usuario o lanza excepción si no tiene.
     *
     * @param userId UUID del usuario
     * @return El carrito activo
     */
    private Cart obtenerCarritoActivo(UUID userId) {
        return cartRepository.findByUserIdAndStatus(userId, EstadoCarrito.ACTIVE)
                .orElseThrow(() -> new BusinessRuleException(
                        "No tienes un carrito activo",
                        "CARRITO_NO_ENCONTRADO"
                ));
    }

    /**
     * Obtiene el carrito activo del usuario o lo crea si no tiene.
     * Garantiza que el usuario siempre tenga un carrito disponible.
     *
     * @param userId UUID del usuario
     * @return El carrito activo (existente o recién creado)
     */
    private Cart obtenerOCrearCarrito(UUID userId) {
        return cartRepository.findByUserIdAndStatus(userId, EstadoCarrito.ACTIVE)
                .orElseGet(() -> cartRepository.save(
                        Cart.builder().userId(userId).status(EstadoCarrito.ACTIVE).build()
                ));
    }

    /**
     * Construye el CartResponse completo a partir de la entidad Cart.
     * Carga los ítems, detecta cambios de precio, y calcula totales.
     *
     * @param cart Entidad del carrito
     * @return DTO completo del carrito
     */
    private CartResponse construirCartResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());

        List<CartItemResponse> itemResponses = items.stream()
                .map(this::construirItemResponse)
                .toList();

        BigDecimal total = itemResponses.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = items.stream().mapToInt(CartItem::getQuantity).sum();
        boolean hasPriceChanges = itemResponses.stream().anyMatch(CartItemResponse::priceChanged);

        return new CartResponse(cart.getId(), itemResponses, total, totalItems, hasPriceChanges);
    }

    /**
     * Construye el CartItemResponse para un ítem del carrito.
     * Carga el nombre e imagen del producto y detecta si el precio cambió.
     *
     * @param item Entidad del ítem del carrito
     * @return DTO del ítem con datos del producto y detección de cambio de precio
     */
    private CartItemResponse construirItemResponse(CartItem item) {
        Product product = productRepository.findById(item.getProductId()).orElse(null);

        String productName = product != null ? product.getName() : "Producto no disponible";
        String imageUrl = productImageRepository
                .findByProductIdAndPrimaryTrue(item.getProductId())
                .map(img -> img.getUrl())
                .orElse(null);

        BigDecimal precioActual = product != null
                ? calcularPrecioFinal(product)
                : item.getUnitPrice();

        boolean priceChanged = precioActual.compareTo(item.getUnitPrice()) != 0;

        return new CartItemResponse(
                item.getId(),
                item.getProductId(),
                productName,
                imageUrl,
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal(),
                priceChanged,
                precioActual
        );
    }

    /**
     * Calcula el precio final con IVA de un producto.
     * Fórmula: basePrice × (1 + ivaPercent / 100)
     *
     * @param product Entidad del producto
     * @return Precio final con IVA redondeado a 2 decimales
     */
    private BigDecimal calcularPrecioFinal(Product product) {
        BigDecimal factor = BigDecimal.ONE.add(
                product.getIvaPercent().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
        );
        return product.getBasePrice().multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }
}
