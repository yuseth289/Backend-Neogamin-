package com.neogaming.address.controller;

import com.neogaming.address.dto.request.AddressRequest;
import com.neogaming.address.dto.response.AddressResponse;
import com.neogaming.address.service.AddressService;
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

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la gestión de direcciones del usuario autenticado.
 *
 * Todos los endpoints requieren JWT válido. El usuario solo puede gestionar
 * sus propias direcciones — el servicio verifica esto en cada operación.
 *
 * Endpoints disponibles:
 *  GET    /users/me/addresses             → Listar mis direcciones
 *  POST   /users/me/addresses             → Crear nueva dirección
 *  GET    /users/me/addresses/{id}        → Ver detalle de una dirección
 *  PUT    /users/me/addresses/{id}        → Actualizar dirección
 *  DELETE /users/me/addresses/{id}        → Eliminar dirección (soft delete)
 *  PATCH  /users/me/addresses/{id}/set-primary → Marcar como principal
 */
@RestController
@RequestMapping("/users/me/addresses")
@RequiredArgsConstructor
@Tag(name = "Direcciones", description = "Gestión de direcciones del usuario autenticado")
@SecurityRequirement(name = "bearerAuth")
public class AddressController {

    private final AddressService addressService;

    /**
     * Retorna todas las direcciones activas del usuario.
     * La dirección principal aparece primera en la lista.
     */
    @GetMapping
    @Operation(
            summary = "Listar mis direcciones",
            description = "Retorna todas las direcciones activas del usuario. La principal aparece primera."
    )
    public ResponseEntity<ApiResponse<List<AddressResponse>>> listar() {
        List<AddressResponse> addresses = addressService.listarPorUsuario(
                SecurityUtils.getCurrentUserId()
        );
        return ResponseEntity.ok(ApiResponse.ok(addresses));
    }

    /**
     * Crea una nueva dirección para el usuario.
     * Si es la primera dirección, se convierte en principal automáticamente.
     */
    @PostMapping
    @Operation(
            summary = "Crear dirección",
            description = "Crea una nueva dirección. Si es la primera del usuario, se marca como principal automáticamente."
    )
    public ResponseEntity<ApiResponse<AddressResponse>> crear(
            @Valid @RequestBody AddressRequest request) {

        AddressResponse response = addressService.crear(request, SecurityUtils.getCurrentUserId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    /**
     * Retorna el detalle de una dirección específica del usuario.
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Ver detalle de dirección",
            description = "Retorna los datos de una dirección específica del usuario autenticado."
    )
    public ResponseEntity<ApiResponse<AddressResponse>> obtener(@PathVariable UUID id) {
        AddressResponse response = addressService.obtenerPorId(
                id, SecurityUtils.getCurrentUserId()
        );
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Actualiza todos los campos de una dirección existente.
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar dirección",
            description = "Actualiza los datos de una dirección existente del usuario."
    )
    public ResponseEntity<ApiResponse<AddressResponse>> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AddressRequest request) {

        AddressResponse response = addressService.actualizar(
                id, request, SecurityUtils.getCurrentUserId()
        );
        return ResponseEntity.ok(ApiResponse.ok("Dirección actualizada correctamente", response));
    }

    /**
     * Elimina una dirección (soft delete). Si era la principal, se reasigna automáticamente.
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar dirección",
            description = "Elimina la dirección. Si era la principal, la más antigua activa se convierte en la nueva principal."
    )
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable UUID id) {
        addressService.eliminar(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    /**
     * Establece una dirección como la principal del usuario.
     * Desmarca automáticamente la que era principal anteriormente.
     */
    @PatchMapping("/{id}/set-primary")
    @Operation(
            summary = "Marcar como dirección principal",
            description = "Establece esta dirección como la principal del usuario. La principal anterior queda desmarcada."
    )
    public ResponseEntity<ApiResponse<AddressResponse>> establecerPrincipal(@PathVariable UUID id) {
        AddressResponse response = addressService.establecerPrincipal(
                id, SecurityUtils.getCurrentUserId()
        );
        return ResponseEntity.ok(ApiResponse.ok("Dirección principal actualizada", response));
    }
}
