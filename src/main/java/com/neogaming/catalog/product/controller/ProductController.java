package com.neogaming.catalog.product.controller;

import com.neogaming.catalog.product.dto.request.ProductImageRequest;
import com.neogaming.catalog.product.dto.request.ProductRequest;
import com.neogaming.catalog.product.dto.response.ProductImageResponse;
import com.neogaming.catalog.product.dto.response.ProductResponse;
import com.neogaming.catalog.product.dto.response.ProductSummaryResponse;
import com.neogaming.catalog.product.service.ProductService;
import com.neogaming.common.enums.EstadoProducto;
import com.neogaming.common.response.ApiResponse;
import com.neogaming.common.response.PageResponse;
import com.neogaming.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST para el catálogo de productos de NeoGaming.
 *
 * Endpoints públicos (sin autenticación):
 *  GET  /products                    → Catálogo paginado (productos ACTIVE)
 *  GET  /products/search             → Búsqueda por texto
 *  GET  /products/category/{id}      → Productos por categoría
 *  GET  /products/{slug}             → Detalle de un producto
 *
 * Endpoints del vendedor (requieren rol SELLER):
 *  GET    /products/me               → Mis productos
 *  POST   /products                  → Crear producto (inicia en DRAFT)
 *  GET    /products/me/{id}          → Ver mi producto (cualquier estado)
 *  PUT    /products/me/{id}          → Actualizar producto
 *  PATCH  /products/me/{id}/publish  → Publicar (DRAFT/PAUSED → ACTIVE)
 *  PATCH  /products/me/{id}/pause    → Pausar (ACTIVE → PAUSED)
 *  DELETE /products/me/{id}          → Eliminar (soft delete → DELETED)
 *
 * Gestión de imágenes (requieren rol SELLER):
 *  POST   /products/me/{id}/images              → Agregar imagen
 *  PATCH  /products/me/{id}/images/{imgId}/primary → Establecer imagen principal
 *  DELETE /products/me/{id}/images/{imgId}      → Eliminar imagen
 */
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Productos", description = "Catálogo de productos y gestión de inventario del vendedor")
public class ProductController {

    private final ProductService productService;

    // ===== CATÁLOGO PÚBLICO =====

    @GetMapping
    @Operation(summary = "Listar catálogo", description = "Retorna todos los productos activos del catálogo con paginación.")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> listarCatalogo(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(productService.listarCatalogoPublico(pageable)));
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar productos", description = "Búsqueda de texto en nombre, descripción y marca del producto.")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> buscar(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(productService.buscar(q, pageable)));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Productos por categoría", description = "Lista productos activos de una categoría específica.")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> listarPorCategoria(
            @PathVariable UUID categoryId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(productService.listarPorCategoria(categoryId, pageable)));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Detalle de producto", description = "Retorna el detalle completo de un producto activo.")
    public ResponseEntity<ApiResponse<ProductResponse>> obtenerPorSlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(productService.obtenerPorSlug(slug)));
    }

    // ===== GESTIÓN DEL VENDEDOR =====

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Mis productos", description = "Lista los productos del vendedor filtrados por estado.")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> listarMisProductos(
            @RequestParam(defaultValue = "ACTIVE") EstadoProducto status,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID sellerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                productService.listarMisProductos(sellerId, status, pageable)
        ));
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Crear producto", description = "Crea un producto en estado DRAFT. No visible públicamente hasta publicarlo.")
    public ResponseEntity<ApiResponse<ProductResponse>> crear(
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.crear(request, SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @GetMapping("/me/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Ver mi producto", description = "Retorna el detalle de un producto propio en cualquier estado.")
    public ResponseEntity<ApiResponse<ProductResponse>> obtenerMiProducto(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                productService.obtenerMiProducto(id, SecurityUtils.getCurrentUserId())
        ));
    }

    @PutMapping("/me/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Actualizar producto", description = "Actualiza los datos de un producto del vendedor.")
    public ResponseEntity<ApiResponse<ProductResponse>> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.actualizar(id, request, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok("Producto actualizado correctamente", response));
    }

    @PatchMapping("/me/{id}/publish")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Publicar producto", description = "Publica un producto DRAFT o PAUSED. Queda visible en el catálogo.")
    public ResponseEntity<ApiResponse<ProductResponse>> publicar(@PathVariable UUID id) {
        ProductResponse response = productService.publicar(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok("Producto publicado correctamente", response));
    }

    @PatchMapping("/me/{id}/pause")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Pausar producto", description = "Pausa un producto activo. Deja de aparecer en el catálogo temporalmente.")
    public ResponseEntity<ApiResponse<ProductResponse>> pausar(@PathVariable UUID id) {
        ProductResponse response = productService.pausar(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok("Producto pausado", response));
    }

    @DeleteMapping("/me/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Eliminar producto", description = "Elimina un producto (soft delete). No recuperable desde la API.")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable UUID id) {
        productService.eliminar(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    // ===== GESTIÓN DE IMÁGENES =====

    @PostMapping("/me/{id}/images")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Agregar imagen", description = "Agrega una imagen al producto. Máximo 10 imágenes por producto.")
    public ResponseEntity<ApiResponse<ProductImageResponse>> agregarImagen(
            @PathVariable UUID id,
            @Valid @RequestBody ProductImageRequest request) {
        ProductImageResponse response = productService.agregarImagen(id, request, SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @PatchMapping("/me/{id}/images/{imageId}/primary")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Establecer imagen principal", description = "Marca una imagen como la imagen principal del producto.")
    public ResponseEntity<ApiResponse<ProductImageResponse>> establecerImagenPrincipal(
            @PathVariable UUID id,
            @PathVariable UUID imageId) {
        ProductImageResponse response = productService.establecerImagenPrincipal(
                id, imageId, SecurityUtils.getCurrentUserId()
        );
        return ResponseEntity.ok(ApiResponse.ok("Imagen principal actualizada", response));
    }

    @DeleteMapping("/me/{id}/images/{imageId}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Eliminar imagen", description = "Elimina una imagen del producto.")
    public ResponseEntity<ApiResponse<Void>> eliminarImagen(
            @PathVariable UUID id,
            @PathVariable UUID imageId) {
        productService.eliminarImagen(id, imageId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
