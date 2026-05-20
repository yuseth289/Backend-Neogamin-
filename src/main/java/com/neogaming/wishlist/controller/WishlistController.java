package com.neogaming.wishlist.controller;

import com.neogaming.common.response.ApiResponse;
import com.neogaming.common.util.SecurityUtils;
import com.neogaming.wishlist.dto.CreateWishlistRequest;
import com.neogaming.wishlist.dto.WishlistResponse;
import com.neogaming.wishlist.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la gestión de listas de deseos en NeoGaming.
 *
 * Endpoints del comprador (requieren JWT):
 *  GET    /wishlists                              → Mis listas
 *  POST   /wishlists                              → Crear lista
 *  GET    /wishlists/{id}                         → Ver mi lista por ID
 *  PUT    /wishlists/{id}                         → Editar nombre/visibilidad
 *  DELETE /wishlists/{id}                         → Eliminar lista
 *  POST   /wishlists/{id}/items/{productId}       → Agregar producto
 *  DELETE /wishlists/{id}/items/{productId}       → Quitar producto
 *
 * Endpoint público (sin autenticación):
 *  GET    /wishlists/public/{id}                  → Ver lista pública por enlace
 */
@RestController
@RequestMapping("/wishlists")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Listas de deseos", description = "Gestión de wishlists del comprador")
public class WishlistController {

    private final WishlistService wishlistService;

    // ===== ENDPOINTS DEL COMPRADOR =====

    /**
     * Lista todas las listas de deseos del comprador autenticado con sus ítems.
     */
    @GetMapping
    @Operation(
            summary = "Mis listas de deseos",
            description = "Retorna todas las listas de deseos del comprador autenticado con sus productos."
    )
    public ResponseEntity<ApiResponse<List<WishlistResponse>>> listarMisListas() {
        List<WishlistResponse> listas = wishlistService.listarMisListas(
                SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(listas));
    }

    /**
     * Crea una nueva lista de deseos para el comprador.
     * Máximo 10 listas por usuario.
     */
    @PostMapping
    @Operation(
            summary = "Crear lista de deseos",
            description = "Crea una nueva lista de deseos. Máximo 10 listas por usuario."
    )
    public ResponseEntity<ApiResponse<WishlistResponse>> crearLista(
            @Valid @RequestBody CreateWishlistRequest request) {
        WishlistResponse response = wishlistService.crearLista(
                request, SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    /**
     * Retorna el detalle de una lista del comprador autenticado.
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Detalle de mi lista",
            description = "Retorna una lista de deseos propia con todos sus productos."
    )
    public ResponseEntity<ApiResponse<WishlistResponse>> obtenerMiLista(@PathVariable UUID id) {
        WishlistResponse response = wishlistService.obtenerMiLista(
                id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Actualiza el nombre o la visibilidad de una lista del comprador.
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Editar lista de deseos",
            description = "Actualiza el nombre o la visibilidad (pública/privada) de la lista."
    )
    public ResponseEntity<ApiResponse<WishlistResponse>> actualizarLista(
            @PathVariable UUID id,
            @Valid @RequestBody CreateWishlistRequest request) {
        WishlistResponse response = wishlistService.actualizarLista(
                id, request, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok("Lista actualizada correctamente", response));
    }

    /**
     * Elimina una lista de deseos del comprador junto con todos sus ítems.
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar lista de deseos",
            description = "Elimina la lista y todos sus productos guardados."
    )
    public ResponseEntity<ApiResponse<Void>> eliminarLista(@PathVariable UUID id) {
        wishlistService.eliminarLista(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    /**
     * Agrega un producto a la lista de deseos del comprador.
     */
    @PostMapping("/{id}/items/{productId}")
    @Operation(
            summary = "Agregar producto a lista",
            description = "Agrega un producto a la lista de deseos. " +
                          "No puede duplicarse el mismo producto en la misma lista."
    )
    public ResponseEntity<ApiResponse<WishlistResponse>> agregarProducto(
            @PathVariable UUID id,
            @PathVariable UUID productId) {
        WishlistResponse response = wishlistService.agregarProducto(
                id, productId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok("Producto agregado a la lista", response));
    }

    /**
     * Elimina un producto de la lista de deseos del comprador.
     */
    @DeleteMapping("/{id}/items/{productId}")
    @Operation(
            summary = "Quitar producto de lista",
            description = "Elimina un producto de la lista de deseos."
    )
    public ResponseEntity<ApiResponse<Void>> eliminarProducto(
            @PathVariable UUID id,
            @PathVariable UUID productId) {
        wishlistService.eliminarProducto(id, productId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    // ===== ENDPOINT PÚBLICO =====

    /**
     * Retorna una lista de deseos pública por su ID (sin autenticación).
     * Permite compartir listas por enlace con otros usuarios.
     */
    @GetMapping("/public/{id}")
    @Operation(
            summary = "Ver lista pública",
            description = "Retorna una lista de deseos pública. Sin autenticación. " +
                          "Solo funciona si la lista tiene visibilidad pública."
    )
    public ResponseEntity<ApiResponse<WishlistResponse>> obtenerListaPublica(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(wishlistService.obtenerListaPublica(id)));
    }
}
