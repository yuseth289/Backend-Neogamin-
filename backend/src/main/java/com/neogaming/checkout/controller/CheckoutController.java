package com.neogaming.checkout.controller;

import com.neogaming.checkout.dto.request.InitCheckoutRequest;
import com.neogaming.checkout.dto.response.CheckoutResponse;
import com.neogaming.checkout.service.CheckoutService;
import com.neogaming.common.response.ApiResponse;
import com.neogaming.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para el proceso de checkout en NeoGaming.
 *
 * Todos los endpoints requieren JWT válido.
 *
 * Endpoints disponibles:
 *  POST   /checkout          → Iniciar checkout desde el carrito activo
 *  GET    /checkout/current  → Ver el checkout activo
 *  DELETE /checkout/current  → Cancelar el checkout activo
 */
@RestController
@RequestMapping("/checkout")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Checkout", description = "Proceso de compra: del carrito al pago")
public class CheckoutController {

    private final CheckoutService checkoutService;

    /**
     * Inicia el checkout a partir del carrito activo del usuario.
     *
     * Reserva el stock de todos los ítems y crea una sesión de pago
     * con expiración de 30 minutos (configurable).
     *
     * Errores posibles:
     *  - 400 si el carrito está vacío
     *  - 400 si ya hay un checkout activo
     *  - 400 si no hay suficiente stock para algún ítem
     */
    @PostMapping
    @Operation(
            summary = "Iniciar checkout",
            description = "Crea una sesión de pago desde el carrito activo. Reserva stock y captura precios."
    )
    public ResponseEntity<ApiResponse<CheckoutResponse>> iniciar(
            @Valid @RequestBody InitCheckoutRequest request) {
        CheckoutResponse response = checkoutService.iniciar(request, SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    /**
     * Retorna el checkout activo (IN_PROGRESS) del usuario.
     * Incluye los minutos restantes antes de que expire.
     */
    @GetMapping("/current")
    @Operation(
            summary = "Ver checkout activo",
            description = "Retorna el checkout en progreso con los ítems, totales y tiempo restante."
    )
    public ResponseEntity<ApiResponse<CheckoutResponse>> obtenerActivo() {
        CheckoutResponse response = checkoutService.obtenerActivo(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Cancela el checkout activo y libera el stock reservado.
     * El usuario puede volver a su carrito para modificarlo.
     */
    @DeleteMapping("/current")
    @Operation(
            summary = "Cancelar checkout",
            description = "Cancela el checkout activo y libera el stock reservado para otros compradores."
    )
    public ResponseEntity<ApiResponse<Void>> cancelar() {
        checkoutService.cancelar(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
