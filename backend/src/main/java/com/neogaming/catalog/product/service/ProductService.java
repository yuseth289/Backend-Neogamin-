package com.neogaming.catalog.product.service;

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
import com.neogaming.common.enums.EstadoProducto;
import com.neogaming.common.exception.BusinessRuleException;
import com.neogaming.common.exception.ResourceNotFoundException;
import com.neogaming.common.response.PageResponse;
import com.neogaming.common.util.SlugUtils;
import com.neogaming.inventory.service.InventoryService;
import com.neogaming.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

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
    private final ProductMapper productMapper;

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
    public PageResponse<ProductSummaryResponse> listarCatalogoPublico(Pageable pageable) {
        Page<ProductSummaryResponse> page = productRepository
                .findByStatus(EstadoProducto.ACTIVE, pageable)
                .map(p -> {
                    String urlPrincipal = obtenerUrlImagenPrincipal(p.getId());
                    return productMapper.toSummaryResponse(p, urlPrincipal);
                });
        return PageResponse.from(page);
    }

    /**
     * Lista productos activos filtrados por categoría.
     *
     * @param categoryId UUID de la categoría
     * @param pageable   Paginación
     * @return Página de productos de esa categoría
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> listarPorCategoria(UUID categoryId, Pageable pageable) {
        Page<ProductSummaryResponse> page = productRepository
                .findByCategoryIdAndStatus(categoryId, EstadoProducto.ACTIVE, pageable)
                .map(p -> {
                    String urlPrincipal = obtenerUrlImagenPrincipal(p.getId());
                    return productMapper.toSummaryResponse(p, urlPrincipal);
                });
        return PageResponse.from(page);
    }

    /**
     * Busca productos por texto en nombre, descripción y marca.
     *
     * @param query    Texto a buscar
     * @param pageable Paginación
     * @return Página de productos que coinciden con la búsqueda
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> buscar(String query, Pageable pageable) {
        Page<ProductSummaryResponse> page = productRepository
                .buscarPorTextoYEstado(query, EstadoProducto.ACTIVE, pageable)
                .map(p -> {
                    String urlPrincipal = obtenerUrlImagenPrincipal(p.getId());
                    return productMapper.toSummaryResponse(p, urlPrincipal);
                });
        return PageResponse.from(page);
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

        return productMapper.toResponse(product, images);
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
            UUID sellerId, EstadoProducto status, Pageable pageable) {

        Page<ProductSummaryResponse> page = productRepository
                .findBySellerIdAndStatus(sellerId, status, pageable)
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
    public ProductResponse obtenerMiProducto(UUID productId, UUID sellerId) {
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
    public ProductResponse crear(ProductRequest request, UUID sellerId) {
        // Verificar que el vendedor está activo (aprobado por admin)
        sellerRepository.findByUserId(sellerId)
                .filter(s -> s.getStatus().name().equals("ACTIVE"))
                .orElseThrow(() -> new BusinessRuleException(
                        "Tu perfil de vendedor debe estar aprobado para crear productos",
                        "VENDEDOR_NO_ACTIVO"
                ));

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
    public ProductResponse actualizar(UUID productId, ProductRequest request, UUID sellerId) {
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
    public ProductResponse publicar(UUID productId, UUID sellerId) {
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
    public ProductResponse pausar(UUID productId, UUID sellerId) {
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
    public void eliminar(UUID productId, UUID sellerId) {
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
    public ProductImageResponse agregarImagen(UUID productId, ProductImageRequest request, UUID sellerId) {
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
    public ProductImageResponse establecerImagenPrincipal(UUID productId, UUID imageId, UUID sellerId) {
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
    public void eliminarImagen(UUID productId, UUID imageId, UUID sellerId) {
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

    /**
     * Genera un slug único combinando el nombre del producto con parte del sellerId.
     * Esto permite que dos vendedores puedan tener productos con el mismo nombre
     * sin colisión de slugs.
     *
     * @param name     Nombre del producto
     * @param sellerId UUID del vendedor
     * @return Slug único garantizado
     */
    private String generarSlugUnico(String name, UUID sellerId) {
        // Usar los primeros 8 caracteres del sellerId como sufijo
        String sellerSuffix = sellerId.toString().substring(0, 8);
        String baseSlug = SlugUtils.toSlug(name) + "-" + sellerSuffix;
        String candidato = baseSlug;
        int contador = 2;

        while (productRepository.existsBySlug(candidato)) {
            candidato = baseSlug + "-" + contador;
            contador++;
        }
        return candidato;
    }
}
