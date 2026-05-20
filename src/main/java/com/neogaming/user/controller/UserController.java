package com.neogaming.user.controller;

import com.neogaming.common.response.ApiResponse;
import com.neogaming.common.util.SecurityUtils;
import com.neogaming.user.dto.request.ChangePasswordRequest;
import com.neogaming.user.dto.request.UpdateProfileRequest;
import com.neogaming.user.dto.response.UserResponse;
import com.neogaming.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión del perfil del usuario autenticado.
 *
 * Todos los endpoints requieren un JWT válido en el header Authorization.
 * El usuario solo puede acceder y modificar su propio perfil.
 *
 * Endpoints disponibles:
 *  GET  /users/me          → Ver mi perfil
 *  PUT  /users/me          → Actualizar mis datos personales
 *  PUT  /users/me/password → Cambiar mi contraseña
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Gestión del perfil del usuario autenticado")
@SecurityRequirement(name = "bearerAuth") // Indica en Swagger que todos los endpoints requieren JWT
public class UserController {

    private final UserService userService;

    /**
     * Retorna el perfil completo del usuario autenticado.
     *
     * El UUID del usuario se extrae automáticamente del token JWT
     * a través de SecurityUtils, sin necesidad de pasarlo por URL.
     */
    @GetMapping("/me")
    @Operation(
            summary = "Ver mi perfil",
            description = "Retorna los datos del perfil del usuario actualmente autenticado"
    )
    public ResponseEntity<ApiResponse<UserResponse>> obtenerMiPerfil() {
        // Obtener el UUID del usuario autenticado desde el SecurityContext
        UserResponse response = userService.obtenerPorId(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Actualiza los datos personales del usuario autenticado.
     * Solo los campos enviados con valor se actualizan (null = sin cambios).
     */
    @PutMapping("/me")
    @Operation(
            summary = "Actualizar mi perfil",
            description = "Actualiza nombre, apellido, teléfono o avatar. Los campos no enviados se conservan."
    )
    public ResponseEntity<ApiResponse<UserResponse>> actualizarMiPerfil(
            @Valid @RequestBody UpdateProfileRequest request) {

        UserResponse response = userService.actualizarPerfil(
                SecurityUtils.getCurrentUserId(), request
        );
        return ResponseEntity.ok(ApiResponse.ok("Perfil actualizado correctamente", response));
    }

    /**
     * Cambia la contraseña del usuario autenticado.
     * Requiere la contraseña actual como verificación de seguridad adicional.
     */
    @PutMapping("/me/password")
    @Operation(
            summary = "Cambiar contraseña",
            description = "Cambia la contraseña del usuario. Se requiere la contraseña actual para confirmar la identidad."
    )
    public ResponseEntity<ApiResponse<Void>> cambiarContrasena(
            @Valid @RequestBody ChangePasswordRequest request) {

        userService.cambiarContrasena(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
