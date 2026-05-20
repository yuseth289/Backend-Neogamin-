package com.neogaming.auth.controller;

import com.neogaming.auth.dto.request.LoginRequest;
import com.neogaming.auth.dto.request.RefreshTokenRequest;
import com.neogaming.auth.dto.request.RegisterRequest;
import com.neogaming.auth.dto.response.TokenResponse;
import com.neogaming.auth.service.AuthService;
import com.neogaming.common.response.ApiResponse;
import com.neogaming.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para autenticación y gestión de sesiones en NeoGaming.
 *
 * Endpoints públicos (no requieren JWT):
 *  POST /auth/register → Registrar nuevo usuario
 *  POST /auth/login    → Iniciar sesión
 *  POST /auth/refresh  → Renovar access token con refresh token
 *
 * Endpoints protegidos (requieren JWT):
 *  POST /auth/logout   → Cerrar sesión (revoca el token actual)
 *
 * Flujo típico de un cliente:
 *  1. POST /auth/register o /auth/login → recibe accessToken + refreshToken
 *  2. Usa accessToken en cada request: "Authorization: Bearer <token>"
 *  3. Cuando el accessToken expira (15 min) → POST /auth/refresh con refreshToken
 *  4. Cuando termina la sesión → POST /auth/logout
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Registro, login, logout y renovación de tokens")
public class AuthController {

    private final AuthService authService;

    /**
     * Registra un nuevo usuario en NeoGaming.
     *
     * El usuario queda con rol CLIENT y estado ACTIVE inmediatamente.
     * Se retorna el par de tokens para acceso directo sin un segundo login.
     *
     * @param request    Datos del nuevo usuario (email, contraseña, nombre, apellido)
     * @param httpRequest Para extraer el User-Agent del cliente (identificación de dispositivo)
     */
    @PostMapping("/register")
    @Operation(
            summary = "Registrar usuario",
            description = "Crea una nueva cuenta de usuario con rol CLIENT. Retorna tokens de acceso inmediatos."
    )
    public ResponseEntity<ApiResponse<TokenResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        // Extraer User-Agent para registrar el dispositivo en la sesión
        String deviceInfo = httpRequest.getHeader("User-Agent");

        TokenResponse tokens = authService.register(request, deviceInfo);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registro exitoso. ¡Bienvenido a NeoGaming!", tokens));
    }

    /**
     * Inicia sesión con email y contraseña.
     *
     * Si las credenciales son correctas, retorna un nuevo par de tokens.
     * Permite múltiples sesiones activas en distintos dispositivos.
     */
    @PostMapping("/login")
    @Operation(
            summary = "Iniciar sesión",
            description = "Autentica al usuario con email y contraseña. Retorna accessToken (15 min) y refreshToken (30 días)."
    )
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String deviceInfo = httpRequest.getHeader("User-Agent");
        TokenResponse tokens = authService.login(request, deviceInfo);
        return ResponseEntity.ok(ApiResponse.ok("Inicio de sesión exitoso", tokens));
    }

    /**
     * Cierra la sesión del usuario autenticado.
     *
     * Revoca la sesión actual en la base de datos. El access token sigue siendo
     * técnicamente válido hasta que expire (máximo 15 min), pero cualquier request
     * con ese token será rechazado por el JwtAuthenticationFilter al detectar
     * que la sesión está revocada.
     *
     * No afecta otras sesiones activas del mismo usuario en otros dispositivos.
     */
    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Cerrar sesión",
            description = "Revoca la sesión activa. El token queda inválido de inmediato."
    )
    public ResponseEntity<ApiResponse<Void>> logout() {
        // Obtener el sessionId del JWT actual y revocarlo
        authService.logout(SecurityUtils.getCurrentSessionId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    /**
     * Renueva el access token usando el refresh token.
     *
     * Implementa rotación de tokens: el refresh token enviado queda invalidado
     * y se emite un nuevo par. Si el mismo refresh token se envía dos veces,
     * el segundo intento es rechazado.
     */
    @PostMapping("/refresh")
    @Operation(
            summary = "Renovar tokens",
            description = "Intercambia un refresh token válido por un nuevo par de tokens. El refresh token usado queda invalidado."
    )
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        TokenResponse tokens = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok("Tokens renovados correctamente", tokens));
    }
}
