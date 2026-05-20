package com.neogaming.cart.controller;

import com.neogaming.cart.dto.request.AddCartItemRequest;
import com.neogaming.cart.dto.response.CartResponse;
import com.neogaming.cart.service.CartService;
import com.neogaming.common.response.ApiResponse;
import com.neogaming.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST para el carrito de compras del usuario autenticado.
 *
 * Todos los endpoints requieren JWT válido.
 *
 * Endpoints disponibles:
 *  GET    /cart                     → Ver carrito actual
 *  POST   /cart/items               → Agregar ítem al carrito
 *  PATCH  /cart/items/{id}?qty={n}  → Actualizar cantidad de un ítem
 *  DELETE /cart/items/{id}          → Eliminar un ítem
 *  DELETE /cart                     → Vaciar el carrito completo
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Carrito", description = "Gestión del carrito de compras del usuario autenticado")
public class CartController {

    private final CartService cartService;

    /**
     * Retorna el carrito activo del usuario con todos sus ítems.
     * Si el usuario no tiene carrito, retorna un carrito vacío.
     * Detecta automáticamente si algún precio cambió.
     */
    @GetMapping
    @Operation(
            summary = "Ver mi carrito",
            description = "Retorna el carrito activo con ítems, subtotales y total. Detecta cambios de precio."
    )
    public ResponseEntity<ApiResponse<CartResponse>> obtenerCarrito() {
        CartResponse response = cartService.obtenerCarrito(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Agrega un producto al carrito. Si ya existe, incrementa la cantidad.
     * El carrito se crea automáticamente si el usuario no tiene uno activo.
     */
    @PostMapping("/items")
    @Operation(
            summary = "Agregar al carrito",
            description = "Agrega un producto al carrito. Si ya existe el producto, suma la cantidad."
    )
    public ResponseEntity<ApiResponse<CartResponse>> agregar(
            @Valid @RequestBody AddCartItemRequest request) {
        CartResponse response = cartService.agregar(request, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok("Producto agregado al carrito", response));
    }

    /**
     * Actualiza la cantidad de un ítem. Si la cantidad es 0, elimina el ítem.
     */
    @PatchMapping("/items/{id}")
    @Operation(
            summary = "Actualizar cantidad",
            description = "Cambia la cantidad de un ítem. quantity=0 elimina el ítem del carrito."
    )
    public ResponseEntity<ApiResponse<CartResponse>> actualizarCantidad(
            @PathVariable UUID id,
            @RequestParam int quantity) {
        CartResponse response = cartService.actualizarCantidad(id, quantity, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Elimina un ítem específico del carrito.
     */
    @DeleteMapping("/items/{id}")
    @Operation(
            summary = "Eliminar ítem",
            description = "Elimina un producto específico del carrito."
    )
    public ResponseEntity<ApiResponse<Void>> eliminarItem(@PathVariable UUID id) {
        cartService.eliminarItem(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    /**
     * Vacía el carrito completo eliminando todos sus ítems.
     * El carrito permanece activo (vacío) para futuros usos.
     */
    @DeleteMapping
    @Operation(
            summary = "Vaciar carrito",
            description = "Elimina todos los ítems del carrito. El carrito sigue activo para futuras compras."
    )
    public ResponseEntity<ApiResponse<Void>> vaciar() {
        cartService.vaciar(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
