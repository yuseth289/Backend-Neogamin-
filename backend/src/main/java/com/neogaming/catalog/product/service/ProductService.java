package com.neogaming.catalog.product.service;

import com.neogaming.catalog.offer.domain.Offer;
import com.neogaming.catalog.offer.repository.OfferRepository;
import com.neogaming.catalog.product.domain.Product;
import com.neogaming.catalog.product.domain.ProductImage;
import com.neogaming.catalog.product.dto.request.ProductImageRequest;
import com.neogaming.catalog.product.dto.request.ProductRequest;
import com.neogaming.catalog.product.dto.response.ProductImageResponse;
import com.neogaming.catalog.product.dto.response.ProductResponse;
import com.neogaming.catalog.product.dto.response.ProductSummaryResponse;
import com.neogaming.catalog.product.mapper.ProductMapper;
import com.neogaming.catalog.product.repository.ProductImageRepository;
import com.neogaming.catalog.product.repository.ProductRepository;
import com.neogaming.common.enums.EstadoGenerico;
import com.neogaming.common.enums.EstadoProducto;
import com.neogaming.common.exception.BusinessRuleException;
import com.neogaming.common.exception.ConflictException;
import com.neogaming.common.exception.ResourceNotFoundException;
import com.neogaming.common.response.PageResponse;
import com.neogaming.common.util.SlugUtils;
import com.neogaming.inventory.service.InventoryService;
import com.neogaming.review.repository.ReviewRepository;
import com.neogaming.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neogaming.seller.domain.Seller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servicio de gestión del catálogo de productos para NeoGaming.
 *
 * Reglas de negocio implementadas:
 *  - Solo vendedores ACTIVOS pueden crear productos
 *  - Un producto nuevo inicia en DRAFT (no visible públicamente)
 *  - El vendedor debe publicarlo explícitamente para que sea ACTIVE
 *  - Un producto ACTIVE puede pausarse (PAUSED) o eliminarse (DELETED)
 *  - El catálogo público solo muestra productos ACTIVE
 *  - Máximo 10 imágenes por producto
 *  - El precio final = basePrice × (1 + ivaPercent/100) — calculado, no persistido
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final SellerRepository sellerRepository;
    private final InventoryService inventoryService;
    private final ReviewRepository reviewRepository;
    private final ProductMapper productMapper;
    private final OfferRepository offerRepository;

    /** Número máximo de imágenes permitidas por producto */
    private static final int MAX_IMAGENES_POR_PRODUCTO = 10;

    // ===== CATÁLOGO PÚBLICO =====

    /**
     * Lista todos los productos activos del catálogo con paginación.
     * Solo visible para compradores — no incluye productos en DRAFT o PAUSED.
     *
     * @param pageable Configuración de paginación y ordenamiento
     * @return Página de productos activos
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> listarCatalogoPublico(UUID sellerId, List<String> brands,
                                                                       BigDecimal minPrice, BigDecimal maxPrice,
                                                                       Pageable pageable) {
        Pageable pg = remapSort(pageable);
        Page<Product> raw = buscarConFiltros(brands, pg,
                brand -> productRepository.findByStatusFiltered(EstadoProducto.ACTIVE, sellerId, brand, minPrice, maxPrice, pg));
        Map<UUID, BigDecimal> discounts = obtenerDescuentosVigentes(raw.getContent());
        Map<UUID, Integer> stocks = obtenerStocksParaProductos(raw.getContent());
        Map<UUID, SellerInfo> tiendas = obtenerInfoTiendas(raw.getContent());
        Map<UUID, RatingInfo> ratings = obtenerRatingsParaProductos(raw.getContent());
        return PageResponse.from(raw.map(p -> {
            SellerInfo t = tiendas.get(p.getSellerId());
            RatingInfo r = ratings.get(p.getId());
            return productMapper.toSummaryResponse(p, obtenerUrlImagenPrincipal(p.getId()),
                    stocks.get(p.getId()), discounts.get(p.getId()),
                    t != null ? t.storeName() : null, t != null ? t.storeSlug() : null,
                    r != null ? r.averageRating() : null, r != null ? r.totalReviews() : null);
        }));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> listarPorCategoria(UUID categoryId, List<String> brands,
                                                                    BigDecimal minPrice, BigDecimal maxPrice,
                                                                    Pageable pageable) {
        Pageable pg = remapSort(pageable);
        Page<Product> raw = buscarConFiltros(brands, pg,
                brand -> productRepository.findByCategoryIdAndStatusFiltered(categoryId, EstadoProducto.ACTIVE, brand, minPrice, maxPrice, pg));
        Map<UUID, BigDecimal> discounts = obtenerDescuentosVigentes(raw.getContent());
        Map<UUID, Integer> stocks = obtenerStocksParaProductos(raw.getContent());
        Map<UUID, SellerInfo> tiendas = obtenerInfoTiendas(raw.getContent());
        Map<UUID, RatingInfo> ratings = obtenerRatingsParaProductos(raw.getContent());
        return PageResponse.from(raw.map(p -> {
            SellerInfo t = tiendas.get(p.getSellerId());
            RatingInfo r = ratings.get(p.getId());
            return productMapper.toSummaryResponse(p, obtenerUrlImagenPrincipal(p.getId()),
                    stocks.get(p.getId()), discounts.get(p.getId()),
                    t != null ? t.storeName() : null, t != null ? t.storeSlug() : null,
                    r != null ? r.averageRating() : null, r != null ? r.totalReviews() : null);
        }));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> buscar(String query, List<String> brands,
                                                       BigDecimal minPrice, BigDecimal maxPrice,
                                                       Pageable pageable) {
        Pageable pg = remapSort(pageable);
        Page<Product> raw = buscarConFiltros(brands, pg,
                brand -> productRepository.buscarFiltrado(query, EstadoProducto.ACTIVE, brand, minPrice, maxPrice, pg));
        Map<UUID, BigDecimal> discounts = obtenerDescuentosVigentes(raw.getContent());
        Map<UUID, Integer> stocks = obtenerStocksParaProductos(raw.getContent());
        Map<UUID, SellerInfo> tiendas = obtenerInfoTiendas(raw.getContent());
        Map<UUID, RatingInfo> ratings = obtenerRatingsParaProductos(raw.getContent());
        return PageResponse.from(raw.map(p -> {
            SellerInfo t = tiendas.get(p.getSellerId());
            RatingInfo r = ratings.get(p.getId());
            return productMapper.toSummaryResponse(p, obtenerUrlImagenPrincipal(p.getId()),
                    stocks.get(p.getId()), discounts.get(p.getId()),
                    t != null ? t.storeName() : null, t != null ? t.storeSlug() : null,
                    r != null ? r.averageRating() : null, r != null ? r.totalReviews() : null);
        }));
    }

    /**
     * Obtiene el detalle completo de un producto activo por su slug.
     * Solo retorna productos en estado ACTIVE para el catálogo público.
     *
     * @param slug Slug del producto
     * @return Detalle completo del producto con todas sus imágenes
     * @throws ResourceNotFoundException si el producto no existe o no está activo
     */
    @Transactional(readOnly = true)
    public ProductResponse obtenerPorSlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .filter(p -> p.getStatus() == EstadoProducto.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", slug));

        List<ProductImage> images = productImageRepository
                .findByProductIdOrderByPrimaryDescSortOrderAsc(product.getId());

        Integer availableStock = inventoryService.obtenerStockDisponible(product.getId());
        BigDecimal discount = offerRepository
                .findOfertaVigente(product.getId(), EstadoGenerico.ACTIVE, Instant.now())
                .map(Offer::getDiscountValue)
                .orElse(null);
        Seller seller = sellerRepository.findById(product.getSellerId()).orElse(null);
        return productMapper.toResponse(product, images, availableStock, discount,
                seller != null ? seller.getStoreName() : null,
                seller != null ? seller.getStoreSlug() : null);
    }

    // ===== GESTIÓN DEL VENDEDOR =====

    /**
     * Lista los productos del vendedor autenticado filtrados por estado.
     * Incluye todos los estados (DRAFT, ACTIVE, PAUSED) excepto DELETED.
     *
     * @param sellerId UUID del vendedor
     * @param status   Estado a filtrar
     * @param pageable Paginación
     * @return Página de productos del vendedor
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> listarMisProductos(
            UUID userId, EstadoProducto status, Pageable pageable) {
        return listarMisProductos(userId, status, null, pageable);
    }

    public PageResponse<ProductSummaryResponse> listarMisProductos(
            UUID userId, EstadoProducto status, String q, Pageable pageable) {

        UUID sellerId = resolverSellerId(userId);
        Page<ProductSummaryResponse> page = (q != null && !q.isBlank()
                ? productRepository.buscarPorSellerYNombre(sellerId, q, pageable)
                : status != null
                    ? productRepository.findBySellerIdAndStatus(sellerId, status, pageable)
                    : productRepository.findBySellerIdAndStatusNot(sellerId, EstadoProducto.DELETED, pageable))
                .map(p -> {
                    String urlPrincipal = obtenerUrlImagenPrincipal(p.getId());
                    return productMapper.toSummaryResponse(p, urlPrincipal);
                });
        return PageResponse.from(page);
    }

    /**
     * Obtiene el detalle de un producto propio del vendedor (cualquier estado).
     *
     * @param productId UUID del producto
     * @param sellerId  UUID del vendedor (para verificar propiedad)
     * @return Detalle completo del producto
     */
    @Transactional(readOnly = true)
    public ProductResponse obtenerMiProducto(UUID productId, UUID userId) {
        UUID sellerId = resolverSellerId(userId);
        Product product = buscarPorIdYVendedor(productId, sellerId);
        List<ProductImage> images = productImageRepository
                .findByProductIdOrderByPrimaryDescSortOrderAsc(productId);
        return productMapper.toResponse(product, images);
    }

    /**
     * Crea un nuevo producto para el vendedor en estado DRAFT.
     *
     * Solo vendedores con status ACTIVE pueden crear productos.
     *
     * @param request  Datos del producto
     * @param sellerId UUID del vendedor propietario
     * @return El producto recién creado en estado DRAFT
     * @throws BusinessRuleException si el vendedor no está activo
     */
    public ProductResponse crear(ProductRequest request, UUID userId) {
        // Verificar que el vendedor está activo (aprobado por admin)
        com.neogaming.seller.domain.Seller seller = sellerRepository.findByUserId(userId)
                .filter(s -> s.getStatus().name().equals("ACTIVE"))
                .orElseThrow(() -> new BusinessRuleException(
                        "Tu perfil de vendedor debe estar aprobado para crear productos",
                        "VENDEDOR_NO_ACTIVO"
                ));

        UUID sellerId = seller.getId();

        if (request.sku() != null && !request.sku().isBlank() &&
                productRepository.existsBySellerIdAndSkuAndStatusNot(sellerId, request.sku(), EstadoProducto.DELETED)) {
            throw new ConflictException("Ya tienes un producto activo con el SKU: " + request.sku(), "SKU_DUPLICADO");
        }

        String slug = generarSlugUnico(request.name(), sellerId);
        BigDecimal ivaPercent = request.ivaPercent() != null
                ? request.ivaPercent()
                : new BigDecimal("19.00");

        Product product = Product.builder()
                .sellerId(sellerId)
                .categoryId(request.categoryId())
                .name(request.name())
                .slug(slug)
                .description(request.description())
                .brand(request.brand())
                .sku(request.sku())
                .basePrice(request.basePrice())
                .ivaPercent(ivaPercent)
                .specifications(request.specifications() != null ? request.specifications() : new java.util.HashMap<>())
                .status(EstadoProducto.DRAFT)
                .build();

        Product saved = productRepository.save(product);
        // Inicializar el registro de inventario con stock 0
        inventoryService.inicializarInventario(saved.getId());
        return productMapper.toResponse(saved, List.of());
    }

    /**
     * Actualiza los datos de un producto del vendedor.
     *
     * Solo se pueden editar productos que NO estén en DELETED.
     *
     * @param productId UUID del producto
     * @param request   Nuevos datos
     * @param sellerId  UUID del vendedor
     * @return El producto actualizado
     */
    public ProductResponse actualizar(UUID productId, ProductRequest request, UUID userId) {
        UUID sellerId = resolverSellerId(userId);
        Product product = buscarPorIdYVendedor(productId, sellerId);

        if (product.getStatus() == EstadoProducto.DELETED) {
            throw new BusinessRuleException(
                    "No se puede editar un producto eliminado",
                    "PRODUCTO_ELIMINADO"
            );
        }

        // Regenerar slug si el nombre cambia
        if (!product.getName().equals(request.name())) {
            product.setSlug(generarSlugUnico(request.name(), sellerId));
            product.setName(request.name());
        }
        if (request.description() != null) product.setDescription(request.description());
        if (request.brand() != null) product.setBrand(request.brand());
        if (request.sku() != null) product.setSku(request.sku());
        if (request.categoryId() != null) product.setCategoryId(request.categoryId());
        if (request.basePrice() != null) product.setBasePrice(request.basePrice());
        if (request.ivaPercent() != null) product.setIvaPercent(request.ivaPercent());
        if (request.specifications() != null) product.setSpecifications(request.specifications());

        List<ProductImage> images = productImageRepository
                .findByProductIdOrderByPrimaryDescSortOrderAsc(productId);
        return productMapper.toResponse(productRepository.save(product), images);
    }

    /**
     * Publica un producto DRAFT o PAUSED → pasa a ACTIVE.
     * Lo hace visible en el catálogo público.
     *
     * @param productId UUID del producto a publicar
     * @param sellerId  UUID del vendedor
     * @return El producto en estado ACTIVE
     */
    public ProductResponse publicar(UUID productId, UUID userId) {
        UUID sellerId = resolverSellerId(userId);
        Product product = buscarPorIdYVendedor(productId, sellerId);

        if (product.getStatus() == EstadoProducto.ACTIVE) {
            throw new BusinessRuleException(
                    "El producto ya está publicado",
                    "PRODUCTO_YA_ACTIVO"
            );
        }
        if (product.getStatus() == EstadoProducto.DELETED) {
            throw new BusinessRuleException(
                    "No se puede publicar un producto eliminado",
                    "PRODUCTO_ELIMINADO"
            );
        }

        product.setStatus(EstadoProducto.ACTIVE);
        List<ProductImage> images = productImageRepository
                .findByProductIdOrderByPrimaryDescSortOrderAsc(productId);
        return productMapper.toResponse(productRepository.save(product), images);
    }

    /**
     * Pausa un producto ACTIVE → pasa a PAUSED.
     * Lo oculta del catálogo sin eliminarlo.
     *
     * @param productId UUID del producto
     * @param sellerId  UUID del vendedor
     * @return El producto en estado PAUSED
     */
    public ProductResponse pausar(UUID productId, UUID userId) {
        UUID sellerId = resolverSellerId(userId);
        Product product = buscarPorIdYVendedor(productId, sellerId);

        if (product.getStatus() != EstadoProducto.ACTIVE) {
            throw new BusinessRuleException(
                    "Solo se pueden pausar productos activos",
                    "ESTADO_INVALIDO_PARA_PAUSAR"
            );
        }

        product.setStatus(EstadoProducto.PAUSED);
        List<ProductImage> images = productImageRepository
                .findByProductIdOrderByPrimaryDescSortOrderAsc(productId);
        return productMapper.toResponse(productRepository.save(product), images);
    }

    /**
     * Elimina un producto (soft delete → DELETED).
     * Un producto eliminado no se puede recuperar desde la API.
     *
     * @param productId UUID del producto
     * @param sellerId  UUID del vendedor
     */
    public void eliminar(UUID productId, UUID userId) {
        UUID sellerId = resolverSellerId(userId);
        Product product = buscarPorIdYVendedor(productId, sellerId);

        if (product.getStatus() == EstadoProducto.DELETED) {
            throw new BusinessRuleException(
                    "El producto ya está eliminado",
                    "PRODUCTO_YA_ELIMINADO"
            );
        }

        product.setStatus(EstadoProducto.DELETED);
        productRepository.save(product);
    }

    // ===== GESTIÓN DE IMÁGENES =====

    /**
     * Agrega una nueva imagen al producto.
     * La primera imagen agregada se convierte automáticamente en la principal.
     * Máximo 10 imágenes por producto.
     *
     * @param productId UUID del producto
     * @param request   Datos de la imagen (URL, alt text, orden)
     * @param sellerId  UUID del vendedor (para verificar propiedad)
     * @return La imagen recién agregada
     * @throws BusinessRuleException si se supera el límite de imágenes
     */
    public ProductImageResponse agregarImagen(UUID productId, ProductImageRequest request, UUID userId) {
        UUID sellerId = resolverSellerId(userId);
        buscarPorIdYVendedor(productId, sellerId);  // Validar propiedad

        long totalImagenes = productImageRepository.countByProductId(productId);
        if (totalImagenes >= MAX_IMAGENES_POR_PRODUCTO) {
            throw new BusinessRuleException(
                    "El producto ya tiene el máximo de " + MAX_IMAGENES_POR_PRODUCTO + " imágenes permitidas",
                    "LIMITE_IMAGENES_ALCANZADO"
            );
        }

        // La primera imagen es automáticamente la principal
        boolean esPrimera = totalImagenes == 0;

        ProductImage image = ProductImage.builder()
                .productId(productId)
                .url(request.url())
                .altText(request.altText())
                .sortOrder(request.sortOrder())
                .primary(esPrimera)
                .build();

        return productMapper.toImageResponse(productImageRepository.save(image));
    }

    /**
     * Establece una imagen como la imagen principal del producto.
     * Desmarca automáticamente la que era principal anteriormente.
     *
     * @param productId UUID del producto
     * @param imageId   UUID de la imagen a establecer como principal
     * @param sellerId  UUID del vendedor
     * @return La imagen marcada como principal
     */
    public ProductImageResponse establecerImagenPrincipal(UUID productId, UUID imageId, UUID userId) {
        UUID sellerId = resolverSellerId(userId);
        buscarPorIdYVendedor(productId, sellerId);  // Validar propiedad

        ProductImage image = productImageRepository
                .findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Imagen", imageId.toString()));

        // Desmarcar la imagen principal actual
        productImageRepository.clearPrimaryByProductId(productId);

        // Marcar la nueva como principal
        image.setPrimary(true);
        return productMapper.toImageResponse(productImageRepository.save(image));
    }

    /**
     * Elimina una imagen del producto.
     * Si se elimina la imagen principal y hay otras imágenes, la primera
     * (por sortOrder) se convierte automáticamente en la nueva principal.
     *
     * @param productId UUID del producto
     * @param imageId   UUID de la imagen a eliminar
     * @param sellerId  UUID del vendedor
     */
    public void eliminarImagen(UUID productId, UUID imageId, UUID userId) {
        UUID sellerId = resolverSellerId(userId);
        buscarPorIdYVendedor(productId, sellerId);  // Validar propiedad

        ProductImage image = productImageRepository
                .findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Imagen", imageId.toString()));

        boolean eraPrincipal = image.isPrimary();
        productImageRepository.delete(image);

        // Si era la principal, asignar la primera imagen restante como nueva principal
        if (eraPrincipal) {
            List<ProductImage> restantes = productImageRepository
                    .findByProductIdOrderByPrimaryDescSortOrderAsc(productId);
            if (!restantes.isEmpty()) {
                restantes.get(0).setPrimary(true);
                productImageRepository.save(restantes.get(0));
            }
        }
    }

    // ===== MÉTODOS AUXILIARES =====

    /**
     * Busca un producto verificando que pertenezca al vendedor.
     *
     * @param productId UUID del producto
     * @param sellerId  UUID del vendedor (usuario autenticado)
     * @return La entidad Product si existe y pertenece al vendedor
     * @throws ResourceNotFoundException si no existe o no pertenece al vendedor
     */
    private UUID resolverSellerId(UUID userId) {
        return sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessRuleException(
                        "No tienes un perfil de vendedor activo",
                        "VENDEDOR_NO_ENCONTRADO"
                ))
                .getId();
    }

    private Product buscarPorIdYVendedor(UUID productId, UUID sellerId) {
        return productRepository.findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", productId.toString()));
    }

    /**
     * Obtiene la URL de la imagen principal de un producto.
     * Retorna null si el producto no tiene imágenes.
     *
     * @param productId UUID del producto
     * @return URL de la imagen principal o null
     */
    private String obtenerUrlImagenPrincipal(UUID productId) {
        return productImageRepository
                .findByProductIdAndPrimaryTrue(productId)
                .map(ProductImage::getUrl)
                .orElse(null);
    }

    private Page<Product> buscarConFiltros(List<String> brands, Pageable pg,
                                           Function<String, Page<Product>> queryFn) {
        boolean hasBrands = brands != null && !brands.isEmpty();
        if (!hasBrands) return queryFn.apply("");
        if (brands.size() == 1) return queryFn.apply(brands.get(0).toLowerCase());
        Set<UUID> seen = new LinkedHashSet<>();
        List<Product> merged = new ArrayList<>();
        long total = 0;
        for (String b : brands) {
            Page<Product> page = queryFn.apply(b.toLowerCase());
            total = Math.max(total, page.getTotalElements());
            for (Product p : page.getContent()) {
                if (seen.add(p.getId())) merged.add(p);
            }
        }
        int maxSize = pg.getPageSize();
        List<Product> content = merged.size() > maxSize ? merged.subList(0, maxSize) : merged;
        return new org.springframework.data.domain.PageImpl<>(content, pg, total);
    }

    private Map<UUID, Integer> obtenerStocksParaProductos(List<Product> productos) {
        if (productos.isEmpty()) return Map.of();
        List<UUID> ids = productos.stream().map(Product::getId).toList();
        return inventoryService.obtenerStocksDisponibles(ids);
    }

    private record SellerInfo(String storeName, String storeSlug) {}

    private record RatingInfo(Double averageRating, Long totalReviews) {}

    private Map<UUID, RatingInfo> obtenerRatingsParaProductos(Collection<Product> productos) {
        if (productos.isEmpty()) return Map.of();
        List<UUID> ids = productos.stream().map(Product::getId).toList();
        return reviewRepository.calcularRatingsParaProductos(ids).stream()
                .collect(Collectors.toMap(
                        ReviewRepository.ProductRating::getProductId,
                        r -> new RatingInfo(r.getAverageRating(), r.getTotalReviews())));
    }

    private Map<UUID, SellerInfo> obtenerInfoTiendas(Collection<Product> productos) {
        if (productos.isEmpty()) return Map.of();
        List<UUID> sellerIds = productos.stream().map(Product::getSellerId).distinct().toList();
        return sellerRepository.findByIdIn(sellerIds).stream()
                .collect(Collectors.toMap(Seller::getId,
                        s -> new SellerInfo(s.getStoreName(), s.getStoreSlug())));
    }

    private Map<UUID, BigDecimal> obtenerDescuentosVigentes(List<Product> productos) {
        if (productos.isEmpty()) return Map.of();
        List<UUID> ids = productos.stream().map(Product::getId).toList();
        return offerRepository.findVigentesByProductIds(ids, EstadoGenerico.ACTIVE, Instant.now())
                .stream()
                .collect(Collectors.toMap(Offer::getProductId, Offer::getDiscountValue,
                        (a, b) -> a.compareTo(b) >= 0 ? a : b));
    }

    /**
     * Genera un slug único combinando el nombre del producto con parte del sellerId.
     * Esto permite que dos vendedores puedan tener productos con el mismo nombre
     * sin colisión de slugs.
     *
     * @param name     Nombre del producto
     * @param sellerId UUID del vendedor
     * @return Slug único garantizado
     */
    /**
     * Traduce campos de ordenamiento del cliente (DTO) a campos reales de la entidad Product.
     * Evita PropertyReferenceException cuando el cliente envía campos calculados o de otras entidades.
     */
    private Pageable remapSort(Pageable pageable) {
        Map<String, String> fieldMap = Map.of(
                "finalPrice",    "basePrice",
                "averageRating", "createdAt",
                "totalReviews",  "createdAt"
        );
        Sort mapped = Sort.by(
                pageable.getSort().stream()
                        .map(o -> new Sort.Order(
                                o.getDirection(),
                                fieldMap.getOrDefault(o.getProperty(), o.getProperty())))
                        .toList()
        );
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                mapped.isSorted() ? mapped : Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private String generarSlugUnico(String name, UUID sellerId) {
        String sellerSuffix = sellerId.toString().substring(0, 8);
        String truncatedName = name.length() > 70 ? name.substring(0, 70) : name;
        String baseSlug = SlugUtils.toSlug(truncatedName) + "-" + sellerSuffix;
        String candidato = baseSlug;
        int contador = 2;

        while (productRepository.existsBySlug(candidato)) {
            candidato = baseSlug + "-" + contador;
            contador++;
        }
        return candidato;
    }
}
