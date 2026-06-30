package com.neogaming.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neogaming.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Punto de entrada para requests sin autenticación válida (token ausente, inválido o expirado).
 *
 * Por defecto, Spring Security con autenticación anónima habilitada responde 403 también
 * para este caso, indistinguible de un usuario autenticado sin el rol requerido
 * (@PreAuthorize). Este entry point corrige eso devolviendo 401, para que el interceptor
 * del frontend pueda distinguir "necesito refrescar el token" (401) de "no tienes permiso"
 * (403, manejado por GlobalExceptionHandler.handleAccessDenied).
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.error("No has iniciado sesión o tu sesión expiró", "UNAUTHORIZED")));
    }
}
