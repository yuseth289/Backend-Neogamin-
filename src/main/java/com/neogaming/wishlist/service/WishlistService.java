package com.neogaming.wishlist.service;

import com.neogaming.catalog.product.domain.Product;
import com.neogaming.catalog.product.repository.ProductImageRepository;
import com.neogaming.catalog.product.repository.ProductRepository;
import com.neogaming.common.exception.BusinessRuleException;
import com.neogaming.common.exception.ConflictException;
import com.neogaming.common.exception.ResourceNotFoundException;
import com.neogaming.inventory.repository.InventoryRepository;
import com.neogaming.wishlist.domain.Wishlist;
import com.neogaming.wishlist.domain.WishlistItem;
import com.neogaming.wishlist.dto.CreateWishlistRequest;
import com.neogaming.wishlist.dto.WishlistItemResponse;
import com.neogaming.wishlist.dto.WishlistResponse;
import com.neogaming.wishlist.repository.WishlistItemRepository;
import com.neogaming.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de gestión de listas de deseos.
 *
 * Un usuario puede tener hasta 10 listas de deseos.
 * Cada lista puede ser privada o pública (compartible por enlace).
 * Un producto solo puede aparecer una vez por lista.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WishlistService {

    /** Máximo de listas de deseos permitidas por usuario */
    private static final int MAX_LISTAS_POR_USUARIO = 10;

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final InventoryRepository inventoryRepository;

    /**
     * Crea una nueva lista de deseos para el usuario.
     * Máximo 10 listas por usuario.
     *
     * @param request Nombre y visibilidad de la lista
     * @param userId  UUID del usuario autenticado
     * @return DTO de la lista creada (vacía)
     */
    public WishlistResponse crearLista(CreateWishlistRequest request, UUID userId) {
        long total = wishlistRepository.countByUserId(userId);
        if (total >= MAX_LISTAS_POR_USUARIO) {
            throw new BusinessRuleException(
                    "Has alcanzado el máximo de " + MAX_LISTAS_POR_USUARIO + " listas de deseos",
                    "LIMITE_WISHLISTS_ALCANZADO"
            );
        }

        Wishlist lista = Wishlist.builder()
                .userId(userId)
                .name(request.name())
                .isPublic(request.isPublic())
                .build();

        Wishlist saved = wishlistRepository.save(lista);
        return toResponse(saved, List.of());
    }

    /**
     * Lista todas las listas de deseos del usuario autenticado.
     *
     * @param userId UUID del usuario autenticado
     * @return Lista de wishlists con sus ítems
     */
    @Transactional(readOnly = true)
    public List<WishlistResponse> listarMisListas(UUID userId) {
        return wishlistRepository.findByUserId(userId).stream()
                .map(lista -> {
                    List<WishlistItem> items = wishlistItemRepository.findByWishlistId(lista.getId());
                    return toResponse(lista, items);
                })
                .toList();
    }

    /**
     * Retorna una lista de deseos pública por su ID.
     * Accesible sin autenticación — para compartir por enlace.
     *
     * @param wishlistId UUID de la lista
     * @return DTO de la lista con sus ítems
     */
    @Transactional(readOnly = true)
    public WishlistResponse obtenerListaPublica(UUID wishlistId) {
        Wishlist lista = wishlistRepository.findByIdAndIsPublicTrue(wishlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Lista de deseos", wishlistId.toString()));
        List<WishlistItem> items = wishlistItemRepository.findByWishlistId(wishlistId);
        return toResponse(lista, items);
    }

    /**
     * Retorna una lista de deseos propia del usuario autenticado.
     *
     * @param wishlistId UUID de la lista
     * @param userId     UUID del usuario autenticado
     * @return DTO de la lista con sus ítems
     */
    @Transactional(readOnly = true)
    public WishlistResponse obtenerMiLista(UUID wishlistId, UUID userId) {
        Wishlist lista = wishlistRepository.findByIdAndUserId(wishlistId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Lista de deseos", wishlistId.toString()));
        List<WishlistItem> items = wishlistItemRepository.findByWishlistId(wishlistId);
        return toResponse(lista, items);
    }

    /**
     * Actualiza el nombre o la visibilidad de una lista del usuario.
     *
     * @param wishlistId UUID de la lista
     * @param request    Nuevos datos de la lista
     * @param userId     UUID del usuario autenticado
     * @return DTO actualizado de la lista
     */
    public WishlistResponse actualizarLista(UUID wishlistId, CreateWishlistRequest request, UUID userId) {
        Wishlist lista = wishlistRepository.findByIdAndUserId(wishlistId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Lista de deseos", wishlistId.toString()));

        lista.setName(request.name());
        lista.setPublic(request.isPublic());

        Wishlist saved = wishlistRepository.save(lista);
        List<WishlistItem> items = wishlistItemRepository.findByWishlistId(wishlistId);
        return toResponse(saved, items);
    }

    /**
     * Elimina una lista de deseos del usuario junto con todos sus ítems.
     *
     * @param wishlistId UUID de la lista
     * @param userId     UUID del usuario autenticado
     */
    public void eliminarLista(UUID wishlistId, UUID userId) {
        Wishlist lista = wishlistRepository.findByIdAndUserId(wishlistId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Lista de deseos", wishlistId.toString()));
        wishlistItemRepository.deleteByWishlistId(lista.getId());
        wishlistRepository.delete(lista);
    }

    /**
     * Agrega un producto a una lista del usuario.
     * El producto ya debe existir y estar activo.
     *
     * @param wishlistId UUID de la lista
     * @param productId  UUID del producto a agregar
     * @param userId     UUID del usuario autenticado
     * @return DTO actualizado de la lista con el nuevo ítem
     */
    public WishlistResponse agregarProducto(UUID wishlistId, UUID productId, UUID userId) {
        Wishlist lista = wishlistRepository.findByIdAndUserId(wishlistId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Lista de deseos", wishlistId.toString()));

        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", productId.toString()));

        if (wishlistItemRepository.existsByWishlistIdAndProductId(wishlistId, productId)) {
            throw new ConflictException(
                    "El producto ya está en esta lista de deseos", "PRODUCTO_YA_EN_WISHLIST");
        }

        WishlistItem item = WishlistItem.builder()
                .wishlistId(wishlistId)
                .productId(productId)
                .build();

        wishlistItemRepository.save(item);

        List<WishlistItem> items = wishlistItemRepository.findByWishlistId(wishlistId);
        return toResponse(lista, items);
    }

    /**
     * Elimina un producto de una lista del usuario.
     *
     * @param wishlistId UUID de la lista
     * @param productId  UUID del producto a eliminar
     * @param userId     UUID del usuario autenticado
     */
    public void eliminarProducto(UUID wishlistId, UUID productId, UUID userId) {
        wishlistRepository.findByIdAndUserId(wishlistId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Lista de deseos", wishlistId.toString()));

        WishlistItem item = wishlistItemRepository
                .findByWishlistIdAndProductId(wishlistId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto en lista", productId.toString()));

        wishlistItemRepository.delete(item);
    }

    // ─── Métodos privados ────────────────────────────────────────────────────

    /** Convierte una Wishlist con sus ítems al DTO de respuesta. */
    private WishlistResponse toResponse(Wishlist lista, List<WishlistItem> items) {
        List<WishlistItemResponse> itemDtos = items.stream()
                .map(this::toItemResponse)
                .toList();
        return new WishlistResponse(
                lista.getId(),
                lista.getName(),
                lista.isPublic(),
                itemDtos,
                lista.getCreatedAt()
        );
    }

    /** Convierte un WishlistItem al DTO de respuesta enriquecido con datos del producto. */
    private WishlistItemResponse toItemResponse(WishlistItem item) {
        Product producto = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto", item.getProductId().toString()));

        String imagenUrl = productImageRepository
                .findByProductIdAndPrimaryTrue(item.getProductId())
                .map(img -> img.getUrl())
                .orElse(null);

        boolean hayStock = inventoryRepository.findByProductId(item.getProductId())
                .map(inv -> inv.getAvailableStock() > 0)
                .orElse(false);

        BigDecimal precioFinal = calcularPrecioFinal(producto.getBasePrice(), producto.getIvaPercent());

        return new WishlistItemResponse(
                item.getId(),
                item.getProductId(),
                producto.getName(),
                producto.getSlug(),
                imagenUrl,
                precioFinal,
                hayStock,
                item.getAddedAt()
        );
    }

    /** Calcula el precio final con IVA: basePrice × (1 + ivaPercent / 100). */
    private BigDecimal calcularPrecioFinal(BigDecimal basePrice, BigDecimal ivaPercent) {
        BigDecimal multiplicador = BigDecimal.ONE.add(
                ivaPercent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        return basePrice.multiply(multiplicador).setScale(2, RoundingMode.HALF_UP);
    }
}
